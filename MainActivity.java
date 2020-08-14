package com.example.firebasedemoproject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity{

    public static final String ARTIST_NAME = "artistname";
    public static final String ARTIST_ID = "artistid";

    EditText et1;
    Button submit;
    Spinner spinner1;
    ListView listViewArtists;

    private TextView textViewUserEmail;
    private Button buttonLogout;
    private FirebaseAuth firebaseAuth;

    DatabaseReference databaseArtists;
    DatabaseReference databaseArtists1;
    FirebaseUser firebaseUser;
    List<Artist> artistList;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        databaseArtists= FirebaseDatabase.getInstance().getReference("artists");
        databaseArtists1= FirebaseDatabase.getInstance().getReference("Users");
        et1= findViewById(R.id.et1);
        submit = findViewById(R.id.submit);
        spinner1 = findViewById(R.id.spinner1);
        listViewArtists = findViewById(R.id.listViewArtists);

        //-------new---------
        firebaseAuth=FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        System.out.println("Current User "+firebaseUser);

        if (firebaseAuth.getCurrentUser() == null)
        {
            startActivity(new Intent(this,SignUpActivity.class));
        }
        FirebaseUser user=firebaseAuth.getCurrentUser();
        textViewUserEmail=findViewById(R.id.textViewUserEmail);
        textViewUserEmail.setText("Welcome "+user.getEmail());
        buttonLogout=findViewById(R.id.buttonLogout);

        //-------------------

        loadUserInformation();

        final Query specific_user = databaseArtists1.child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        specific_user.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        //here you will get the data
                        System.out.println("Here is "+specific_user);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });

        buttonLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                firebaseAuth.signOut();
                finish();
                Intent i=new Intent(getApplicationContext(),SignUpActivity.class);
                startActivity(i);
            }
        });

        artistList=new ArrayList<>();

        listViewArtists.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Artist artist=artistList.get(i);

                Intent intent = new Intent(getApplicationContext(),AddTrackActivity.class);
                intent.putExtra(ARTIST_ID,artist.getArtistId());
                intent.putExtra(ARTIST_NAME,artist.getArtistName());

                startActivity(intent);
            }
        });
        listViewArtists.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                Artist artist=artistList.get(i);
                showUpdateDialog(artist.getArtistId(),artist.getArtistName());
                return false;
            }
        });
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addArtist();
            }
        });
    }
    @Override
    protected void onStart() {
        super.onStart();
        databaseArtists1.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot data : dataSnapshot.getChildren()) {
                    String userName = String.valueOf(data.child("userType").getValue());
                    Toast.makeText(MainActivity.this, userName, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        databaseArtists.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                artistList.clear();
                for (DataSnapshot artistSnapshot: dataSnapshot.getChildren())
                {
                    Artist artist=artistSnapshot.getValue(Artist.class);
                    artistList.add(artist);
                }
                ArtistList adapter= new ArtistList(MainActivity.this,artistList);
                listViewArtists.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void loadUserInformation()
    {
        FirebaseUser user = firebaseAuth.getCurrentUser();

        String userType = user.getUid();

        Toast.makeText(this, userType, Toast.LENGTH_SHORT).show();
    }
    private void showUpdateDialog(final String artistId, String artistName)
    {
        AlertDialog.Builder dialogBuilder=new AlertDialog.Builder(this);
        LayoutInflater inflater=getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.update_dialog,null);
        dialogBuilder.setView(dialogView);

        final EditText editTextNAme=(EditText)dialogView.findViewById(R.id.editTextName);
        final Button buttonUpdate=(Button)dialogView.findViewById(R.id.buttonUpdate);
        final Button buttonDelete=(Button)dialogView.findViewById(R.id.buttonDelete);
        final Spinner spinnerGenres=(Spinner)dialogView.findViewById(R.id.spinnerGenres);

        dialogBuilder.setTitle("Updating Artist "+artistName);

        final AlertDialog alertDialog=dialogBuilder.create();
        alertDialog.show();

        buttonUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name=editTextNAme.getText().toString().trim();
                String genre = spinnerGenres.getSelectedItem().toString();

                if (TextUtils.isEmpty(name))
                {
                    editTextNAme.setError("Name required");
                    return;
                }
                updateArtist(artistId,name,genre);
                alertDialog.dismiss();
            }
        });

        buttonDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteArtist(artistId);

                alertDialog.dismiss();
            }
        });

    }
    private void deleteArtist(String artistId)
    {
        DatabaseReference drArtist=FirebaseDatabase.getInstance().getReference("artists").child(artistId);

        drArtist.removeValue();

        Toast.makeText(this, "Track Deleted Successfully", Toast.LENGTH_SHORT).show();
    }
    private boolean updateArtist(String id, String name, String genre)
    {
        DatabaseReference databaseReference=FirebaseDatabase.getInstance().getReference("artists").child(id);
        Artist artist=new Artist(id,name,genre);
        databaseReference.setValue(artist);

        Toast.makeText(this, "Artist Updated Successfully", Toast.LENGTH_SHORT).show();
        return true;
    }
    private void addArtist()
    {
        String name = et1.getText().toString().trim();
        String genres = spinner1.getSelectedItem().toString();

        if (!TextUtils.isEmpty(name))
        {
            String id = databaseArtists.push().getKey();
            Artist artist=new Artist( id , name , genres );

            databaseArtists.child(id).setValue(artist);
            Toast.makeText(this, "Artist Added Successfully", Toast.LENGTH_SHORT).show();
        }
        else
        {
            Toast.makeText(this, "Please Enter Your Name", Toast.LENGTH_SHORT).show();
        }
    }
}
