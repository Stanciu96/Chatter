package com.chatter.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.chatter.DAO.ChatterDatabase;
import com.chatter.DAO.MediaDAO;
import com.chatter.R;
import com.chatter.classes.Media;
import com.chatter.classes.Message;
import com.chatter.classes.User;
import com.chatter.viewHolders.MessageViewHolder;
import com.chatter.viewHolders.MessageWithMediaViewHolder;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

//TODO: BUFFER GLOBAL IN ACTIVITATE PENTRU A TRIMITE TEXT SI POZA IN ACELAS TIMP
public class MessagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
    private final int NO_MEDIA = 0, WITH_MEDIA = 1;
    private final ArrayList<Message> messages;
    public MessagesAdapter(ArrayList<Message> messages) {
        this.messages = messages;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        RecyclerView.ViewHolder viewHolder;
        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());

        switch (viewType) {
            case WITH_MEDIA:
                View mediaView = inflater.inflate(R.layout.message_with_media_view, viewGroup, false);
                viewHolder = new MessageWithMediaViewHolder(mediaView);
                break;
            case NO_MEDIA:
                View textView = inflater.inflate(R.layout.message_view, viewGroup, false);
                viewHolder = new MessageViewHolder(textView);
                break;
            default:
                View defaultView = inflater.inflate(R.layout.message_view, viewGroup, false);
                viewHolder = new MessageViewHolder(defaultView);
                break;
        }
        return viewHolder;

    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        switch (viewHolder.getItemViewType()) {
            case NO_MEDIA:
                MessageViewHolder vh1 = (MessageViewHolder) viewHolder;
                bindWithOutMedia(vh1, position);
                break;
            case WITH_MEDIA:
                MessageWithMediaViewHolder vh2 = (MessageWithMediaViewHolder) viewHolder;
                bindWithMedia(vh2, position);
                break;

        }
    }

    private void bindWithMedia(MessageWithMediaViewHolder viewHolder, int position){
        Message message = messages.get(position);
        viewHolder.getTextViewMessageSender().setText(message.getSenderEmail());
        viewHolder.getTextViewMessageContent().setText(message.getTextContent());

        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd hh:mm");
        viewHolder.getTextViewMessageTimestamp().setText(dateFormat.format(messages.get(position).getTimestamp()));
        if(position != 0 ){
            if(messages.get(position -1 ).getSenderEmail().equals(message.getSenderEmail())) {
                viewHolder.getTextViewMessageSender().setVisibility(View.INVISIBLE);
            }
        }

        if (User.getEmail().equals(message.getSenderEmail())) {
            viewHolder.getTextViewMessageSender().setVisibility(View.GONE);//ascunde numele meu
            RelativeLayout.LayoutParams params =
                    (RelativeLayout.LayoutParams) viewHolder.getRelativeLayout().getLayoutParams();
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);//aliniere la dreapta
            viewHolder.getRelativeLayout().setLayoutParams(params);
        } else {
            RelativeLayout.LayoutParams params =
                    (RelativeLayout.LayoutParams) viewHolder.getRelativeLayout().getLayoutParams();
            params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);//aliniere la dreapta
            viewHolder.getRelativeLayout().setLayoutParams(params);
        }

        //verificare daca exista in room
        //daca exista afiseaza
        //daca nu exista, descarca, salveaza si afiseaza
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                Context context = viewHolder.itemView.getContext();
                ChatterDatabase db = Room.databaseBuilder(context, ChatterDatabase.class, "media-database").build();
                MediaDAO mediaDAO = db.mediaDAO();
                Media media = mediaDAO.getByLink(messages.get(position).getMediaKey());
                if(media !=null){
                    //daca exista local
                    Bitmap img = BitmapFactory.decodeByteArray(media.data, 0, media.data.length);
                    viewHolder.getImageView().setImageBitmap(img);
                } else {
                    getMediaFromFirebase(messages.get(position).getMediaKey(), viewHolder);
                }

            }
        });
        viewHolder.getImageView().setVisibility(View.VISIBLE);

    }

    private void bindWithOutMedia(MessageViewHolder viewHolder, int position){
        Message message = messages.get(position);
        viewHolder.getTextViewMessageSender().setText(message.getSenderEmail());
        viewHolder.getTextViewMessageContent().setText(message.getTextContent());

        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd hh:mm");
        viewHolder.getTextViewMessageTimestamp().setText(dateFormat.format(messages.get(position).getTimestamp()));
        if(position != 0 ){
            if(messages.get(position -1 ).getSenderEmail().equals(message.getSenderEmail())) {
                viewHolder.getTextViewMessageSender().setVisibility(View.INVISIBLE);
            }
        }

        if (User.getEmail().equals(message.getSenderEmail())) {
            viewHolder.getTextViewMessageSender().setVisibility(View.GONE);//ascunde numele meu
            RelativeLayout.LayoutParams params =
                    (RelativeLayout.LayoutParams) viewHolder.getRelativeLayout().getLayoutParams();
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);//aliniere la dreapta
            viewHolder.getRelativeLayout().setLayoutParams(params);
        } else {
            RelativeLayout.LayoutParams params =
                    (RelativeLayout.LayoutParams) viewHolder.getRelativeLayout().getLayoutParams();
            params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);//aliniere la dreapta
            viewHolder.getRelativeLayout().setLayoutParams(params);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (messages.get(position).getContainsMedia()) {
            return WITH_MEDIA;
        } else if (!messages.get(position).getContainsMedia()) {
            return NO_MEDIA;
        }
        return -1;
    }

    public void getMediaFromFirebase(String mediaLink, MessageWithMediaViewHolder viewHolder){
        //descarcare din firebase si salvare in local
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference imageRef = storage.getReference().child(mediaLink);

        File localFile = null;
        try {
            localFile = File.createTempFile("images", "jpg");
        } catch (IOException e) {
            e.printStackTrace();
        }

        File finalLocalFile = localFile;
        imageRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                Bitmap img = BitmapFactory.decodeFile(finalLocalFile.getAbsolutePath());
                viewHolder.getImageView().setImageBitmap(img);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                img.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                byte[] mediaBytes = baos.toByteArray();

                Media media = new Media(imageRef.getPath(), 0, mediaBytes);
                saveMedia(media, viewHolder.itemView.getContext());
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
            }
        });
    }
    @Override
    public int getItemCount() {
        return messages.size();
    }

    //salvarea fisierelor media in baza de date room
    private void saveMedia(Media media, Context context) {
        ChatterDatabase db = Room.databaseBuilder(context,
                ChatterDatabase.class, "media-database").build();

        MediaDAO mediaDAO = db.mediaDAO();
        mediaDAO.insertMedia(media);
    }


}