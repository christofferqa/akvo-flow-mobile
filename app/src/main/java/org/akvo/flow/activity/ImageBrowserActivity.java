/*
 *  Copyright (C) 2010-2014 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo FLOW.
 *
 *  Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 *  the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 *  either version 3 of the License or any later version.
 *
 *  Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License included below for more details.
 *
 *  The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 */

package org.akvo.flow.activity;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import org.akvo.flow.R;
import org.akvo.flow.util.ConstantUtil;
import org.akvo.flow.util.FileUtil;
import org.akvo.flow.util.FileUtil.FileType;
import org.akvo.flow.util.PropertyUtil;
import org.akvo.flow.ui.adapter.HelpImageBrowserAdapter;

/**
 * Activity to show image help files with their captions. Clicking an item in
 * the gallery at the top will switch the image (and its caption) in the main
 * panel. <b>this activity expects that the extras bundle contains 2 ArrayLists
 * of the same size (one containing image urls, the other containing their
 * captions).</b>
 * 
 * @author Christopher Fagiani
 */
public class ImageBrowserActivity extends Activity implements
        OnItemClickListener {
    private Gallery gallery;
    private ImageView mainImageView;
    private TextView captionTextView;
    private HelpImageBrowserAdapter imageAdapter;
    private ArrayList<String> imageUrls;
    private ArrayList<String> captions;
    private String surveyId;
    private PropertyUtil props;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.imagebrowser);
        props = new PropertyUtil(getResources());
        gallery = (Gallery) findViewById(R.id.imagebrowsergallery);
        captionTextView = (TextView) findViewById(R.id.captiontextview);
        mainImageView = (ImageView) findViewById(R.id.mainimageview);

        Bundle extras = getIntent().getExtras();
        imageUrls = extras.getStringArrayList(ConstantUtil.IMAGE_URL_LIST_KEY);
        captions = extras
                .getStringArrayList(ConstantUtil.IMAGE_CAPTION_LIST_KEY);
        surveyId = extras.getString(ConstantUtil.SURVEY_ID_KEY);
        File cacheDir = new File(FileUtil.getFilesDir(FileType.FORMS), surveyId);
        if (!cacheDir.exists()) {
            cacheDir.mkdir();
        }
        imageAdapter = new HelpImageBrowserAdapter(this, imageUrls, cacheDir.getAbsolutePath());
        if (imageUrls.size() == 1) {
            gallery.setVisibility(View.GONE);
        } else {
            Toast.makeText(this, R.string.imagehelpviewmessage,
                    Toast.LENGTH_LONG).show();
            gallery.setVisibility(View.VISIBLE);
        }

        gallery.setAdapter(imageAdapter);
        gallery.setOnItemClickListener(this);
        mainImageView.setImageBitmap(imageAdapter.getImageBitmap(0));
        captionTextView.setText(captions.get(0));
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void onItemClick(AdapterView parent, View v, int position, long id) {
        mainImageView.setImageBitmap(imageAdapter.getImageBitmap(position));
        captionTextView.setText(captions.get(position));
    }

}
