package com.chatter.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.chatter.R;
import com.chatter.activities.ConversationActivity;
import com.chatter.classes.Contact;
import com.chatter.classes.Conversation;
import com.chatter.classes.Message;
import com.chatter.classes.User;


public class ConversationsAdapter extends RecyclerView.Adapter<ConversationsAdapter.ViewHolder> {

    public ConversationsAdapter() {
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.conversation_view, viewGroup, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, final int position) {
        Conversation conversation = User.getConversations().get(position);
        if (conversation.getParticipantsList().size() == 2) {
            for (Contact contact : conversation.getParticipantsList()) {
                if (!contact.getEmail().equals(User.getEmail())) {
                    viewHolder.getTextViewConversationTitle().setText(contact.getEmail());
                    break;
                }
            }
        } else {
            viewHolder.getTextViewConversationTitle().setText(conversation.getName());
        }

        if (!conversation.getMessagesList().isEmpty()) {
            Message lastMessage = conversation.getMessagesList().get(conversation.getMessagesList().size() - 1);
            viewHolder.getTextViewLastMessageSender().setText(lastMessage.getSenderEmail());
            viewHolder.getTextViewLastMessageContent().setText(lastMessage.getContent());
        }


        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(v.getContext(), User.getConversations().get(position).getName(), Toast.LENGTH_SHORT).show();
                Intent openConversationIntent = new Intent(v.getContext(), ConversationActivity.class);
                openConversationIntent.putExtra("conversation_key", User.getConversations().get(position).getKey());
                v.getContext().startActivity(openConversationIntent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return User.getConversations().size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textViewConversationTitle;
        private final TextView textViewLastMessageSender;
        private final TextView textViewLastMessageContent;
        private final TextView textViewLastMessageTimestamp;

        public ViewHolder(View view) {
            super(view);
            textViewConversationTitle = view.findViewById(R.id.textViewConversationTitle);
            textViewLastMessageSender = view.findViewById(R.id.textViewLastMessageSender);
            textViewLastMessageContent = view.findViewById(R.id.textViewLastMessageContent);
            textViewLastMessageTimestamp = view.findViewById(R.id.textViewLastMessageTimestamp);
        }

        public TextView getTextViewConversationTitle() {
            return textViewConversationTitle;
        }

        public TextView getTextViewLastMessageSender() {
            return textViewLastMessageSender;
        }

        public TextView getTextViewLastMessageContent() {
            return textViewLastMessageContent;
        }

        public TextView getTextViewLastMessageTimestamp() {
            return textViewLastMessageTimestamp;
        }
    }
}