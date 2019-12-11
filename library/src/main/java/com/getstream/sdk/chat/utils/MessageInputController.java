package com.getstream.sdk.chat.utils;

import android.app.Activity;
import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.getstream.sdk.chat.R;
import com.getstream.sdk.chat.adapter.AttachmentListAdapter;
import com.getstream.sdk.chat.adapter.CommandMentionListItemAdapter;
import com.getstream.sdk.chat.adapter.MediaAttachmentAdapter;
import com.getstream.sdk.chat.adapter.MediaAttachmentSelectedAdapter;
import com.getstream.sdk.chat.databinding.StreamViewMessageInputBinding;
import com.getstream.sdk.chat.enums.InputType;
import com.getstream.sdk.chat.enums.MessageInputType;
import com.getstream.sdk.chat.model.Attachment;
import com.getstream.sdk.chat.model.Channel;
import com.getstream.sdk.chat.model.Command;
import com.getstream.sdk.chat.model.Member;
import com.getstream.sdk.chat.model.ModelType;
import com.getstream.sdk.chat.rest.User;
import com.getstream.sdk.chat.rest.interfaces.UploadFileCallback;
import com.getstream.sdk.chat.rest.response.UploadFileResponse;
import com.getstream.sdk.chat.view.MessageInputStyle;
import com.getstream.sdk.chat.view.MessageInputView;
import com.getstream.sdk.chat.viewmodel.ChannelViewModel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MessageInputController {

    private static final String TAG = MessageInputController.class.getSimpleName();

    private ChannelViewModel viewModel;
    private Channel channel;
    private MessageInputStyle style;
    private MediaAttachmentAdapter mediaAttachmentAdapter = null;
    private MediaAttachmentSelectedAdapter selectedMediaAttachmentAdapter = null;
    private CommandMentionListItemAdapter<MessageInputStyle> commandMentionListItemAdapter;
    private List<Object> commands = null;
    private Context context;
    private StreamViewMessageInputBinding binding;
    private AttachmentListAdapter fileAttachmentAdapter = null;
    private AttachmentListAdapter selectedFileAttachmentAdapter = null;
    private MessageInputType messageInputType;
    private List<Attachment> selectedAttachments = null;
    private MessageInputView.AttachmentListener attachmentListener;
    private boolean uploadingFile = false;
    private List<Attachment> localAttachments;

    // region Attachment

    public MessageInputController(@NonNull Context context,
                                  @NonNull StreamViewMessageInputBinding binding,
                                  @NonNull ChannelViewModel viewModel,
                                  @NonNull MessageInputStyle style,
                                  @Nullable MessageInputView.AttachmentListener attachmentListener) {
        this.context = context;
        this.binding = binding;
        this.viewModel = viewModel;
        this.channel = viewModel.getChannel();
        this.style = style;
        this.attachmentListener = attachmentListener;
    }

    public List<Attachment> getSelectedAttachments() {
        return selectedAttachments;
    }

    public boolean isUploadingFile() {
        return uploadingFile;
    }

    public void setSelectedAttachments(List<Attachment> selectedAttachments) {
        this.selectedAttachments = selectedAttachments;
    }

    public void onClickOpenBackGroundView(MessageInputType type) {

        binding.getRoot().setBackgroundResource(R.drawable.stream_round_thread_toolbar);
        binding.clTitle.setVisibility(View.VISIBLE);
        binding.btnClose.setVisibility(View.VISIBLE);

        binding.clAddFile.setVisibility(View.GONE);
        binding.clCommand.setVisibility(View.GONE);
        binding.clSelectPhoto.setVisibility(View.GONE);

        switch (type){
            case EDIT_MESSAGE:
                break;
            case ADD_FILE:
                if (selectedAttachments != null && !selectedAttachments.isEmpty()) return;
                binding.clAddFile.setVisibility(View.VISIBLE);
                break;
            case UPLOAD_MEDIA:
            case UPLOAD_FILE:
                binding.clSelectPhoto.setVisibility(View.VISIBLE);
                break;
            case COMMAND:
            case MENTION:
                binding.btnClose.setVisibility(View.GONE);
                binding.clCommand.setVisibility(View.VISIBLE);
                break;
        }
        binding.tvTitle.setText(type.getLabel());
        messageInputType = type;
        configPermissions();
    }

    public void configPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            binding.ivMediaPermission.setVisibility(View.GONE);
            binding.ivCameraPermission.setVisibility(View.GONE);
            binding.ivFilePermission.setVisibility(View.GONE);
            return;
        }

        if (PermissionChecker.isGrantedCameraPermissions(context)) {
            binding.ivMediaPermission.setVisibility(View.GONE);
            binding.ivCameraPermission.setVisibility(View.GONE);
            binding.ivFilePermission.setVisibility(View.GONE);
        } else if (PermissionChecker.isGrantedStoragePermissions(context)) {
            binding.ivMediaPermission.setVisibility(View.GONE);
            binding.ivCameraPermission.setVisibility(View.VISIBLE);
            binding.ivFilePermission.setVisibility(View.GONE);
        } else {
            binding.ivMediaPermission.setVisibility(View.VISIBLE);
            binding.ivCameraPermission.setVisibility(View.VISIBLE);
            binding.ivFilePermission.setVisibility(View.VISIBLE);
        }
    }

    public void onClickCloseBackGroundView() {
        binding.clTitle.setVisibility(View.GONE);
        binding.clAddFile.setVisibility(View.GONE);
        binding.clSelectPhoto.setVisibility(View.GONE);
        binding.clCommand.setVisibility(View.GONE);
        binding.getRoot().setBackgroundResource(0);
        messageInputType = null;
        commandMentionListItemAdapter = null;
    }

    private void initLoadAttachemtView() {
        binding.rvComposer.setVisibility(View.GONE);
        binding.lvComposer.setVisibility(View.GONE);
        selectedMediaAttachmentAdapter = null;
        selectedFileAttachmentAdapter = null;
        selectedAttachments = null;
        binding.progressBarFileLoader.setVisibility(View.VISIBLE);
    }

    // endregion

    // region Upload Attachment File

    private void configSelectAttachView(List<Attachment> editAttachments, boolean isMedia) {
        binding.setIsAttachFile(!isMedia);
        
        selectedAttachments = (editAttachments != null) ? editAttachments : new ArrayList<>();        
        localAttachments = getAttachmentsFromLocal(isMedia);
        
        ((Activity) context).runOnUiThread(() -> {
            if (!localAttachments.isEmpty()){
                configGalleryView(isMedia);
            }else{
                Utils.showMessage(context, context.getResources().getString(R.string.stream_no_media_error));
                onClickCloseBackGroundView();
            }
            binding.progressBarFileLoader.setVisibility(View.GONE);
            if (editAttachments != null) {
                showHideComposerAttachmentGalleryView(true, isMedia);
                setSelectedAttachmentAdapter(isMedia);
            }
        });
    }

    private List<Attachment> getAttachmentsFromLocal(boolean isMedia) {
        if (isMedia)
            return Utils.getMediaAttachments(context);

        Utils.attachments = new ArrayList<>();
        return Utils.getFileAttachments(Environment.getExternalStorageDirectory());
    }
    
    private void configGalleryView(boolean isMedia){
        if (isMedia){
            mediaAttachmentAdapter = new MediaAttachmentAdapter(context, localAttachments, position ->
                uploadOrCancelAttachment(localAttachments.get(position), true, isMedia)
            );
            binding.rvMedia.setAdapter(mediaAttachmentAdapter);
        }else {
            fileAttachmentAdapter = new AttachmentListAdapter(context, localAttachments, true, true);
            binding.lvFile.setAdapter(fileAttachmentAdapter);
            binding.lvFile.setOnItemClickListener((AdapterView<?> parent, View view,
                                                   int position, long id) ->
                uploadOrCancelAttachment(localAttachments.get(position),true, isMedia)
            );
        }
    }

    private void uploadOrCancelAttachment(Attachment attachment,
                                          boolean fromGallery,
                                          boolean isMedia){

        attachment.config.setSelected(!attachment.config.isSelected());
        if (attachment.config.isSelected()) {
            if (UploadManager.isOverMaxUploadFileSize(new File(attachment.config.getFilePath()), true))
                return;
            if (selectedAttachments == null)
                selectedAttachments = new ArrayList<>();
            selectedAttachments.add(attachment);
            uploadFile(attachment, fromGallery, isMedia);
            if (fromGallery)
                totalAttachmentAdaterChanged(attachment, isMedia);

            showHideComposerAttachmentGalleryView(true, isMedia);
            selectedAttachmentAdapderChanged(attachment, isMedia);
        } else{
            selectedAttachments.remove(attachment);
            if (fromGallery)
                totalAttachmentAdaterChanged(null, isMedia);
            selectedAttachmentAdapderChanged(null, isMedia);
        }
        configCannotSendMessageButton();
    }

    private void showHideComposerAttachmentGalleryView(boolean show, boolean isMedia){
        if (isMedia)
            binding.rvComposer.setVisibility(show ? View.VISIBLE : View.GONE);
        else
            binding.lvComposer.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public void onClickOpenSelectMediaView(List<Attachment> editAttachments) {
        if (!PermissionChecker.isGrantedStoragePermissions(context)) {
            PermissionChecker.showPermissionSettingDialog(context, context.getString(R.string.stream_storage_permission_message));
            return;
        }
        initLoadAttachemtView();
        AsyncTask.execute(() -> configSelectAttachView(editAttachments, true));
        onClickOpenBackGroundView(MessageInputType.UPLOAD_MEDIA);
    }

    private void uploadFile(Attachment attachment,
                            boolean fromGallery,
                            boolean isMedia) {
        uploadingFile = true;
        UploadFileCallback callback = getUploadFileCallBack(attachment, fromGallery, isMedia);
        if (isMedia && attachment.getType().equals(ModelType.attach_image))
            channel.sendImage(attachment.config.getFilePath(), "image/jpeg", callback);
        else
            channel.sendFile(attachment.config.getFilePath(), attachment.getMime_type(), callback);
    }

    private UploadFileCallback getUploadFileCallBack(Attachment attachment,
                                                     boolean fromGallery,
                                                     boolean isMedia) {
        return new UploadFileCallback<UploadFileResponse, Integer>() {
            @Override
            public void onSuccess(UploadFileResponse response) {
                fileUploadSuccess(attachment, response, isMedia);
            }

            @Override
            public void onError(String errMsg, int errCode) {
                fileUploadFailed(attachment, errMsg, fromGallery, isMedia);
            }

            @Override
            public void onProgress(Integer percentage) {
                fileUploading(attachment, percentage, isMedia);
            }
        };
    }

    private void fileUploadSuccess(Attachment attachment,
                                   UploadFileResponse response,
                                   boolean isMedia) {
        uploadingFile = false;
        if (!attachment.config.isSelected())
            return;
        // disable Send button if uploading attachment files and invalid text.
        new Handler().postDelayed(()->{
            if (!uploadingFile)
                setSendButtonState(true);
        },100);


        if (isMedia && attachment.getType().equals(ModelType.attach_image)) {
            File file = new File(attachment.config.getFilePath());
            attachment.setImageURL(response.getFileUrl());
            attachment.setFallback(file.getName());
        } else {
            attachment.setAssetURL(response.getFileUrl());
        }

        attachment.config.setUploaded(true);
        selectedAttachmentAdapderChanged(null, isMedia);

        if (attachmentListener != null)
            attachmentListener.onAddAttachment(attachment);
    }

    private void fileUploadFailed(Attachment attachment,
                                  String errMsg,
                                  boolean fromGallery,
                                  boolean isMedia) {
        uploadingFile = false;
        Utils.showMessage(context, errMsg);
        uploadOrCancelAttachment(attachment, fromGallery, isMedia);
    }

    private void fileUploading(Attachment attachment,
                               Integer percentage,
                               boolean isMedia) {
        uploadingFile = true;
        attachment.config.setProgress(percentage);
        selectedAttachmentAdapderChanged(attachment, isMedia);
        if (StringUtility.isEmptyTextMessage(binding.etMessage.getText().toString()))
            setSendButtonState(false);
    }

    private void setSelectedAttachmentAdapter(boolean isMedia) {
        if (isMedia) {
            selectedMediaAttachmentAdapter = new MediaAttachmentSelectedAdapter(context, selectedAttachments, attachment ->
                uploadOrCancelAttachment(attachment,true, isMedia));
            binding.rvComposer.setAdapter(selectedMediaAttachmentAdapter);
        } else {
            selectedFileAttachmentAdapter = new AttachmentListAdapter(context, selectedAttachments, true, false, attachment ->
                    uploadOrCancelAttachment(attachment,true, isMedia));
            binding.lvComposer.setAdapter(selectedFileAttachmentAdapter);
        }
    }

    private void totalAttachmentAdaterChanged(@Nullable Attachment attachment, boolean isMedia){
        if (isMedia){
            if (attachment == null)
                mediaAttachmentAdapter.notifyDataSetChanged();
            else{
                int index = localAttachments.indexOf(attachment);
                if (index != -1)
                    mediaAttachmentAdapter.notifyItemChanged(index);
            }
        }
        else
            fileAttachmentAdapter.notifyDataSetChanged();

    }

    private void selectedAttachmentAdapderChanged(@Nullable Attachment attachment, boolean isMedia){
        if (isMedia){
            if (selectedMediaAttachmentAdapter == null)
                setSelectedAttachmentAdapter(isMedia);
            if (attachment != null){
                int index = selectedAttachments.indexOf(attachment);
                if (index != -1)
                    selectedMediaAttachmentAdapter.notifyItemChanged(index);

            }else
                selectedMediaAttachmentAdapter.notifyDataSetChanged();
        }
        else{
            if (selectedFileAttachmentAdapter == null)
                setSelectedAttachmentAdapter(isMedia);
            selectedFileAttachmentAdapter.notifyDataSetChanged();
        }
    }

    private void configCannotSendMessageButton(){
        if (selectedAttachments.size() == 0
                && StringUtility.isEmptyTextMessage(binding.etMessage.getText().toString())) {
            viewModel.setInputType(InputType.DEFAULT);
            setSendButtonState(false);
        }
    }

    private void setSendButtonState(boolean enable){
        binding.setActiveMessageSend(enable);
    }

    public void onClickOpenSelectFileView(List<Attachment> editAttachments) {
        if (!PermissionChecker.isGrantedStoragePermissions(context)) {
            PermissionChecker.showPermissionSettingDialog(context, context.getString(R.string.stream_storage_permission_message));
            return;
        }
        initLoadAttachemtView();
        AsyncTask.execute(() -> configSelectAttachView(editAttachments, false));
        onClickOpenBackGroundView(MessageInputType.UPLOAD_FILE);
    }

    public void initSendMessage() {
        binding.etMessage.setText("");
        selectedAttachments = new ArrayList<>();

        binding.lvComposer.removeAllViewsInLayout();
        binding.rvComposer.removeAllViewsInLayout();

        binding.lvComposer.setVisibility(View.GONE);
        binding.rvComposer.setVisibility(View.GONE);

        selectedFileAttachmentAdapter = null;
        onClickCloseBackGroundView();
    }
    // endregion

    // region Camera

    public void progressCapturedMedia(File file, boolean isImage) {
        Attachment attachment = new Attachment();
        attachment.config.setFilePath(file.getPath());
        attachment.setFile_size((int) file.length());
        if (isImage) {
            attachment.setType(ModelType.attach_image);
        } else {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(context, Uri.fromFile(file));
            String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            long videolengh = Long.parseLong(time);
            attachment.config.setVideoLengh((int) (videolengh / 1000));
            Utils.configFileAttachment(attachment, file, ModelType.attach_file, ModelType.attach_mime_mp4);
            retriever.release();
        }
        uploadOrCancelAttachment(attachment,false, true);
    }
    // endregion

    // region Cammand
    private void openCommandView() {
        onClickOpenBackGroundView(MessageInputType.COMMAND);
    }

    private void closeCommandView() {
        if (isCommandOrMention())
            onClickCloseBackGroundView();
        commands = null;
    }

    private boolean isCommandOrMention(){
        return messageInputType != null && ((messageInputType == MessageInputType.COMMAND)
                || (messageInputType == MessageInputType.MENTION));
    }

    public void checkCommand(String text) {
        if (TextUtils.isEmpty(text)
                || (!text.startsWith("/") && !text.contains("@"))) {
            closeCommandView();
        }else if (text.length() == 1) {
            onClickCommandViewOpen(text.startsWith("/"));
        } else if (text.endsWith("@")) {
            onClickCommandViewOpen(false);
        } else {
            setCommandsMentionUsers(text);
            if (!commands.isEmpty() && binding.clCommand.getVisibility() != View.VISIBLE)
                openCommandView();

            setCommandMentionListItemAdapter(text.startsWith("/"));
        }

        if (commands == null || commands.isEmpty())
            closeCommandView();

    }

    private void onClickCommandViewOpen(boolean isCommand) {
        if (isCommand) {
            setCommands("");
        } else {
            setMentionUsers("");
        }
        String title = binding.tvTitle.getContext().getResources().getString(isCommand ? R.string.stream_input_type_command : R.string.stream_input_type_auto_mention);
        binding.tvTitle.setText(title);
        binding.tvCommand.setText("");
        setCommandMentionListItemAdapter(isCommand);

        openCommandView();
        binding.lvCommand.setOnItemClickListener((AdapterView<?> adapterView, View view, int position, long l) -> {
            if (isCommand)
                binding.etMessage.setText("/" + ((Command) commands.get(position)).getName() + " ");
            else {
                String messageStr = binding.etMessage.getText().toString();
                String userName = ((User) commands.get(position)).getName();
                String converted = StringUtility.convertMentionedText(messageStr, userName);
                binding.etMessage.setText(converted);
            }
            binding.etMessage.setSelection(binding.etMessage.getText().length());
            closeCommandView();
        });
    }
    private void setCommandMentionListItemAdapter(boolean isCommand){
        if (commandMentionListItemAdapter  == null) {
            commandMentionListItemAdapter = new CommandMentionListItemAdapter(this.context, commands, style, isCommand);
            binding.lvCommand.setAdapter(commandMentionListItemAdapter);
        }else{
            commandMentionListItemAdapter.setCommand(isCommand);
            commandMentionListItemAdapter.setCommands(commands);
            commandMentionListItemAdapter.notifyDataSetChanged();
        }
    }
    private void setCommandsMentionUsers(String string) {
        if (commands == null) commands = new ArrayList<>();
        commands.clear();
        if (string.startsWith("/")) {
            List<Command>commands = channel.getConfig().getCommands();
            if (commands == null || commands.isEmpty()) return;

            String commandStr = string.replace("/", "");
            setCommands(commandStr);
            binding.tvCommand.setText(commandStr);
        } else {
            String[] names = string.split("@");
            if (names.length > 0)
                setMentionUsers(names[names.length - 1]);
        }
    }

    private void setCommands(String string) {
        if (commands == null) commands = new ArrayList<>();
        commands.clear();
        for (int i = 0; i < channel.getConfig().getCommands().size(); i++) {
            Command command = channel.getConfig().getCommands().get(i);
            if (command.getName().contains(string))
                commands.add(command);
        }
    }

    private void setMentionUsers(String string) {
        Log.d(TAG, "Mention UserName: " + string);
        if (commands == null) commands = new ArrayList<>();
        commands.clear();
        List<Member> members = channel.getChannelState().getMembers();
        for (int i = 0; i < members.size(); i++) {
            Member member = members.get(i);
            User user = member.getUser();
            if (user.getName().toLowerCase().contains(string.toLowerCase()))
                commands.add(user);
        }
    }
    // endregion
}
