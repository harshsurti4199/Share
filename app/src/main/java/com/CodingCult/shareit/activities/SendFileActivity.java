
package com.CodingCult.shareit.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.Button;

import com.CodingCult.shareit.R;
import com.CodingCult.shareit.UriInterpretation;

import java.io.File;
import java.net.URLDecoder;
import java.util.ArrayList;

public class SendFileActivity extends BaseActivity {

    ArrayList<UriInterpretation> uriList = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_file);

        setupToolbar();
        setupTextViews();
        setupNavigationViews();
        createViewClickListener();

        uriList = getFileUris();

        populateUriPath(uriList);
        initHttpServer(uriList);
        saveServerUrlToClipboard();
        setLinkMessageToView();
        setupShareContainingFolderButton(uriList);
    }

    private void setupShareContainingFolderButton(ArrayList<UriInterpretation> uriList){
        Button shareContainingFolderButton = (Button) findViewById(R.id.button_share_containing_folder);

        if( buttonShareContainingFolderShouldBeVisible(uriList)){
            shareContainingFolderButton.setVisibility(View.VISIBLE);
        }else{
            shareContainingFolderButton.setVisibility(View.GONE);
        }

        shareContainingFolderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickButtonShareContainingFolder();
            }
        });
    }

    private void onClickButtonShareContainingFolder() {
        String path = uriList.get(0).getPath();
        int pos = path.lastIndexOf(File.separator);
        if (pos <= 0) {
            Snackbar.make(findViewById(android.R.id.content), "Error getting parent directory.", Snackbar.LENGTH_LONG).show();
            return;
        }

        String newPath = path.substring(0, pos);

        newPath = URLDecoder.decode(newPath);



        Uri theNewUri = Uri.parse(newPath);
        ArrayList<UriInterpretation> newUriArray = new ArrayList<>();
        newUriArray.add(new UriInterpretation(theNewUri, this.getContentResolver()));
        Snackbar.make(findViewById(android.R.id.content), "We are now sharing [" + newPath + "]", Snackbar.LENGTH_LONG).show();

        uriList = newUriArray;
        httpServer.setFiles(newUriArray);
        populateUriPath(newUriArray);
    }

    private boolean buttonShareContainingFolderShouldBeVisible(ArrayList<UriInterpretation> uriList){
        if(uriList.size() != 1)
            return false;

        String uriPath = uriList.get(0).getPath();
        if( uriPath == null || uriPath.length() == 0 ) {
            return false;
        }
        return uriPath.startsWith(File.separator) || uriPath.startsWith("file:");
    }


    private ArrayList<UriInterpretation> getFileUris() {
        Intent dataIntent = getIntent();
        ArrayList<UriInterpretation> theUris = new ArrayList<>();

        if (Intent.ACTION_SEND_MULTIPLE.equals(dataIntent.getAction())) {
            return getUrisForActionSendMultiple(dataIntent, theUris);
        }

        Bundle extras = dataIntent.getExtras();

        Uri myUri = (Uri) extras.get(Intent.EXTRA_STREAM);

        if (myUri == null) {
            String tempString = (String) extras.get(Intent.EXTRA_TEXT);
            if (tempString == null) {
                Snackbar.make(findViewById(android.R.id.content), "Error obtaining the file path", Snackbar.LENGTH_LONG).show();
                return null;
            }

            myUri = Uri.parse(tempString);

            if (myUri == null) {
                Snackbar.make(findViewById(android.R.id.content), "Error obtaining the file path", Snackbar.LENGTH_LONG).show();
                return null;
            }
        }

        theUris.add(new UriInterpretation(myUri, this.getContentResolver()));
        return theUris;
    }

    private ArrayList<UriInterpretation> getUrisForActionSendMultiple(Intent dataIntent, ArrayList<UriInterpretation> theUris) {
        ArrayList<Parcelable> list = dataIntent
                .getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        if (list != null) {
            for (Parcelable parcelable : list) {
                Uri stream = (Uri) parcelable;
                if (stream != null) {
                    theUris.add(new UriInterpretation(stream, this.getContentResolver()));
                }
            }
        }
        return theUris;
    }
}