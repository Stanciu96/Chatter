package com.chatter.dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.chatter.R;
import com.chatter.activities.ContactListActivity;
import com.chatter.adapters.ContactsAdapter;
import com.chatter.classes.Contact;
import com.chatter.classes.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class AddContactDialog extends Dialog implements
        android.view.View.OnClickListener {

    public Activity c;
    public Dialog d;
    public Button buttonAddContact, buttonCancel;
    public User currentUser;
    String newContactEmail;

    public AddContactDialog(Activity a, User currentUser) {
        super(a);
        this.c = a;
        this.currentUser = currentUser;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_add_contact);

        buttonAddContact = findViewById(R.id.button_add_contact);
        buttonCancel= findViewById(R.id.button_cancel_add_contact);
        buttonAddContact.setOnClickListener(this);
        buttonCancel.setOnClickListener(this);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_add_contact:
                addContact(v);
                break;
            case R.id.button_cancel_add_contact:
                dismiss();
                break;
            default:
                break;
        }
        dismiss();
    }

    private void addContact(View v) {
        EditText editTextEmail = findViewById(R.id.edit_text_new_contact_email);
        newContactEmail = editTextEmail.getText().toString();

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference usersRef = database.getReference("users");

        Query query = usersRef.orderByChild("email").equalTo(newContactEmail).limitToFirst(1);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    DataSnapshot userSnapshot = snapshot.getChildren().iterator().next();
                    Contact newContact = userSnapshot.getValue(Contact.class);
                    assert newContact != null;
                    newContact.setKey(userSnapshot.getKey());
                    currentUser.getContacts().add(newContact);

                    FirebaseDatabase database = FirebaseDatabase.getInstance();
                    DatabaseReference contactsRef = database.getReference("users").child(currentUser.getKey()).child("contacts");

                    contactsRef.updateChildren((currentUser.getContactsHashMap()));
                    Intent intent = new Intent();
                    intent.setAction(ContactListActivity.REFRESH_LIST);

                    c.sendBroadcast(intent);
                }
                else {
                    Toast.makeText(v.getContext(), "Emailul introdus este gresit sau utilizatorul nu este inregistrat in aplicatie!",Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


    }

}