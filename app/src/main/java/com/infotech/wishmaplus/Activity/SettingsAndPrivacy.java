package com.infotech.wishmaplus.Activity;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.infotech.wishmaplus.PageAccess.ui.PageAccessActivity;
import com.infotech.wishmaplus.PageAccess.ui.PendingInvitesActivity;
import com.infotech.wishmaplus.R;

public class SettingsAndPrivacy extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings_and_privacy);
        LinearLayout accessLinkLayout = findViewById(R.id.accessLinkL);
        LinearLayout pageSetUpL = findViewById(R.id.pageSetUpL);
        LinearLayout pageInviteL = findViewById(R.id.pageInviteL);
        TextView accessLinkTv = findViewById(R.id.accessLinkTv);
        String accessLink = getIntent().getStringExtra("accessLink");
        boolean isProfileType = getIntent().getBooleanExtra("isProfileType", false);
        String pageId = (getIntent() != null
                && getIntent().hasExtra("pageId")
                && getIntent().getStringExtra("pageId") != null
                && !getIntent().getStringExtra("pageId").trim().isEmpty())
                ? getIntent().getStringExtra("pageId")
                : "";
        if (!isProfileType) {
            pageSetUpL.setVisibility(VISIBLE);
            pageInviteL.setVisibility(VISIBLE);
        } else {
            pageSetUpL.setVisibility(GONE);
            pageInviteL.setVisibility(VISIBLE);
        }
        pageSetUpL.setOnClickListener(v -> {
           Intent intent = new Intent(this, PageAccessActivity.class);
           intent.putExtra("isProfileType", isProfileType);
           intent.putExtra("pageId", pageId);
           startActivity(intent);
        });
        pageInviteL.setOnClickListener(v -> {
            Intent intent = new Intent(this, PendingInvitesActivity.class);
            intent.putExtra("isProfileType", isProfileType);
            intent.putExtra("pageId", pageId);
            startActivity(intent);
        });
        if (accessLink != null && !accessLink.trim().isEmpty()) {
            accessLinkLayout.setVisibility(VISIBLE);
            accessLinkTv.setText(accessLink);
            accessLinkTv.setTextColor(
                    ContextCompat.getColor(this, android.R.color.holo_blue_dark)
            );
            accessLinkTv.setPaintFlags(
                    accessLinkTv.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG
            );
            accessLinkLayout.setOnClickListener(v -> {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, accessLink);
                startActivity(Intent.createChooser(
                        shareIntent, "Share Access Link"
                ));
            });
        } else {
            accessLinkLayout.setVisibility(GONE);
        }
        findViewById(R.id.back_button).setOnClickListener(view -> finish());
        findViewById(R.id.blockingButton).setOnClickListener(view -> startActivity(new Intent(this, BlockedUser.class)));
        findViewById(R.id.deleteButton).setOnClickListener(view -> startActivity(new Intent(this, DeleteAccount.class)));
        findViewById(R.id.adsButton).setOnClickListener(view -> startActivity(new Intent(this, Advertisement.class)));
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}