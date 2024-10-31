package com.haut.wdlm;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    private List<Message> messageList;
    private Context context;

    public MessageAdapter(Context context, List<Message> messageList) {
        this.context = context;
        this.messageList = messageList;
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        if (viewType == 1) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_sent, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_received, parent, false);
        }
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MessageViewHolder holder, int position) {
        Message message = messageList.get(position);
        holder.bind(message, position);
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public class MessageViewHolder extends RecyclerView.ViewHolder {
        private TextView textViewMessageLeft;
        private TextView textViewMessageRight;
        private ImageView imageViewRight;
        private ImageButton buttonCopy;
        private ImageButton buttonRefresh;

        public MessageViewHolder(View itemView) {
            super(itemView);
            textViewMessageLeft = itemView.findViewById(R.id.textViewMessageLeft);
            textViewMessageRight = itemView.findViewById(R.id.textViewMessageRight);
            imageViewRight = itemView.findViewById(R.id.imageViewRight);
            buttonCopy = itemView.findViewById(R.id.copy);
            buttonRefresh = itemView.findViewById(R.id.refresh);
        }

        public void bind(final Message message, final int position) {
            if (message.isSent()) {
                if (textViewMessageRight != null) {
                    if (message.getContent() != null && !message.getContent().isEmpty()) {
                        textViewMessageRight.setText(message.getContent());
                        textViewMessageRight.setVisibility(View.VISIBLE);
                    } else {
                        textViewMessageRight.setVisibility(View.GONE);
                    }
                }
                if (imageViewRight != null) {
                    if (message.getImage() != null) {
                        imageViewRight.setImageBitmap(message.getImage());
                        imageViewRight.setVisibility(View.VISIBLE);
                    } else {
                        imageViewRight.setVisibility(View.GONE);
                    }
                }
                if (textViewMessageLeft != null) {
                    textViewMessageLeft.setVisibility(View.GONE);
                }
                if (buttonCopy != null) {
                    buttonCopy.setVisibility(View.GONE);
                }
                if (buttonRefresh != null) {
                    buttonRefresh.setVisibility(View.GONE);
                }
            } else {
                if (textViewMessageLeft != null) {
                    textViewMessageLeft.setText(message.getContent());
                    textViewMessageLeft.setVisibility(View.VISIBLE);
                }
                if (textViewMessageRight != null) {
                    textViewMessageRight.setVisibility(View.GONE);
                }
                if (imageViewRight != null) {
                    imageViewRight.setVisibility(View.GONE);
                }
                if (buttonCopy != null) {
                    buttonCopy.setVisibility(View.VISIBLE);
                    buttonCopy.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                            ClipData clip = ClipData.newPlainText("copied text", message.getContent());
                            clipboard.setPrimaryClip(clip);
                            Toast.makeText(context, "复制成功！", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                if (buttonRefresh != null) {
                    buttonRefresh.setVisibility(View.VISIBLE);
                    buttonRefresh.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // Remove the current message
                            messageList.remove(position);
                            notifyItemRemoved(position);
                            // Simulate receiving a new message
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    Message newMessage = new Message(message.getContent(), null, false);
                                    messageList.add(position, newMessage);
                                    notifyItemInserted(position);
                                }
                            }, 1000);
                        }
                    });
                }
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messageList.get(position);
        return message.isSent() ? 1 : 0;
    }
}