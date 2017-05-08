package us.koller.cameraroll.adapter.main.ViewHolder;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import us.koller.cameraroll.R;
import us.koller.cameraroll.adapter.SelectorModeManager;
import us.koller.cameraroll.data.Album;
import us.koller.cameraroll.data.FileOperations.Delete;
import us.koller.cameraroll.data.FileOperations.FileOperation;
import us.koller.cameraroll.data.File_POJO;
import us.koller.cameraroll.ui.AlbumActivity;
import us.koller.cameraroll.ui.FileOperationDialogActivity;
import us.koller.cameraroll.ui.MainActivity;
import us.koller.cameraroll.ui.ThemeableActivity;
import us.koller.cameraroll.ui.widget.EqualSpacesItemDecoration;
import us.koller.cameraroll.util.MediaType;
import us.koller.cameraroll.util.StorageUtil;
import us.koller.cameraroll.util.Util;
import us.koller.cameraroll.util.animators.ColorFade;

public class NestedRecyclerViewAlbumHolder extends AlbumHolder
        implements Toolbar.OnMenuItemClickListener {

    private static int SINGLE_LINE_MAX_ITEM_COUNT = 4;

    public RecyclerView nestedRecyclerView;

    public Album album;

    public int sharedElementReturnPosition = -1;

    private EqualSpacesItemDecoration itemDecoration;

    private SelectorModeManager manager;

    private SelectorModeManager.OnBackPressedCallback onBackPressedCallback
            = new SelectorModeManager.OnBackPressedCallback() {
        @Override
        public void cancelSelectorMode() {
            NestedRecyclerViewAlbumHolder.this.cancelSelectorMode();
        }
    };

    private SelectorModeManager.Callback callback
            = new SelectorModeManager.Callback() {
        @Override
        public void onSelectorModeEnter() {
            final View rootView = ((Activity) nestedRecyclerView.getContext())
                    .findViewById(R.id.root_view);

            final Toolbar toolbar = (Toolbar) rootView.findViewById(R.id.toolbar);

            toolbar.setActivated(false);

            Util.setDarkStatusBarIcons(rootView);

            View.OnClickListener onClickListener
                    = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    cancelSelectorMode();
                }
            };

            //create selector-toolbar
            final Toolbar selectorToolbar = SelectorModeUtil.getSelectorModeToolbar(
                    getContext(), onClickListener,
                    NestedRecyclerViewAlbumHolder.this);

            selectorToolbar.setPadding(toolbar.getPaddingLeft(),
                    toolbar.getPaddingTop(),
                    toolbar.getPaddingRight(),
                    toolbar.getPaddingBottom());

            //add selector-toolbar
            ((ViewGroup) toolbar.getParent()).addView(selectorToolbar,
                    toolbar.getLayoutParams());

            selectorToolbar.requestLayout();

            //animate selector-toolbar
            selectorToolbar.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            selectorToolbar.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                            selectorToolbar.setTranslationY(-selectorToolbar.getHeight());
                            selectorToolbar.animate().translationY(0);
                        }
                    });
        }

        @Override
        public void onSelectorModeExit() {
            final View rootView = ((Activity) nestedRecyclerView.getContext())
                    .findViewById(R.id.root_view);

            //find selector-toolbar
            final Toolbar selectorToolbar = (Toolbar) rootView
                    .findViewWithTag(SelectorModeUtil.SELECTOR_TOOLBAR_TAG);

            //animate selector-toolbar
            selectorToolbar.animate()
                    .translationY(-selectorToolbar.getHeight())
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);

                            if (MainActivity.THEME != ThemeableActivity.LIGHT) {
                                Util.setLightStatusBarIcons(rootView);
                            } else if (MainActivity.THEME == ThemeableActivity.LIGHT) {
                                Toolbar toolbar = (Toolbar) rootView.findViewById(R.id.toolbar);
                                toolbar.setActivated(true);
                            }

                            //remove selector-toolbar
                            ((ViewGroup) rootView).removeView(selectorToolbar);
                        }
                    });
        }

        @Override
        public void onItemSelected(int selectedItemCount) {
            final View rootView = ((Activity) nestedRecyclerView.getContext())
                    .findViewById(R.id.root_view);

            final Toolbar toolbar = (Toolbar) rootView
                    .findViewWithTag(SelectorModeUtil.SELECTOR_TOOLBAR_TAG);

            final String title = String.valueOf(selectedItemCount) + (selectedItemCount > 1 ?
                    getContext().getString(R.string.items) : getContext().getString(R.string.item));

            ColorFade.fadeToolbarTitleColor(toolbar,
                    ContextCompat.getColor(getContext(), R.color.grey_900_translucent),
                    new ColorFade.ToolbarTitleFadeCallback() {
                        @Override
                        public void setTitle(Toolbar toolbar) {
                            toolbar.setTitle(title);
                        }
                    }, true);
        }
    };

    public NestedRecyclerViewAlbumHolder(View itemView) {
        super(itemView);

        nestedRecyclerView = (RecyclerView) itemView.findViewById(R.id.nestedRecyclerView);
        if (nestedRecyclerView != null) {
            itemDecoration = new EqualSpacesItemDecoration(
                    (int) getContext().getResources().getDimension(R.dimen.album_grid_spacing), 2, true);
            nestedRecyclerView.addItemDecoration(itemDecoration);
        }

        ((TextView) itemView.findViewById(R.id.name))
                .setTextColor(ContextCompat.getColor(getContext(),
                        ThemeableActivity.text_color_res));

        ((TextView) itemView.findViewById(R.id.count))
                .setTextColor(ContextCompat.getColor(getContext(),
                        ThemeableActivity.text_color_secondary_res));
    }

    public NestedRecyclerViewAlbumHolder setSelectorModeManager(SelectorModeManager manager) {
        this.manager = manager;
        if (!manager.onBackPressedCallbackAlreadySet()) {
            manager.setOnBackPressedCallback(onBackPressedCallback);
        }
        if (manager.getCallback() == null) {
            manager.setCallback(callback);
        }
        return this;
    }

    @Override
    public void setAlbum(Album album) {
        super.setAlbum(album);

        this.album = album;

        //album not excluded
        String count = album.getAlbumItems().size()
                + (album.getAlbumItems().size() > 1 ?
                getContext().getString(R.string.items) :
                getContext().getString(R.string.item));
        ((TextView) itemView.findViewById(R.id.count)).setText(Html.fromHtml(count));

        //make RecyclerView either single ore double lined, depending on the album size
        int lineCount = album.getAlbumItems().size() > SINGLE_LINE_MAX_ITEM_COUNT ? 2 : 1;
        int lineHeight = (int) getContext().getResources()
                .getDimension(R.dimen.nested_recyclerView_line_height) * lineCount;

        LinearLayout.LayoutParams params
                = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, lineHeight);
        nestedRecyclerView.setLayoutParams(params);

        itemDecoration.setSpanCount(lineCount);

        RecyclerView.LayoutManager layoutManager;
        if (album.getAlbumItems().size() > SINGLE_LINE_MAX_ITEM_COUNT) {
            layoutManager = new GridLayoutManager(getContext(), lineCount,
                    GridLayoutManager.HORIZONTAL, false);
        } else {
            layoutManager = new LinearLayoutManager(getContext(),
                    LinearLayoutManager.HORIZONTAL, false);
        }
        nestedRecyclerView.setLayoutManager(layoutManager);

        RecyclerViewAdapter adapter = new RecyclerViewAdapter(callback,
                nestedRecyclerView, album, false);
        adapter.setSelectorModeManager(manager);
        nestedRecyclerView.setAdapter(adapter);
    }

    public interface StartSharedElementTransitionCallback {
        void startPostponedEnterTransition();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void onSharedElement(final int sharedElementReturnPosition,
                                final StartSharedElementTransitionCallback callback) {
        this.sharedElementReturnPosition = sharedElementReturnPosition;

        //to prevent: requestLayout() improperly called [...] during layout: running second layout pass
        nestedRecyclerView.getViewTreeObserver()
                .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        nestedRecyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                        nestedRecyclerView.scrollToPosition(sharedElementReturnPosition);
                    }
                });

        nestedRecyclerView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int l, int t, int r, int b,
                                       int oL, int oT, int oR, int oB) {
                nestedRecyclerView.removeOnLayoutChangeListener(this);
                callback.startPostponedEnterTransition();
            }
        });
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        final String[] paths = ((RecyclerViewAdapter) nestedRecyclerView.getAdapter())
                .cancelSelectorMode();

        cancelSelectorMode();

        Activity a;
        if (getContext() instanceof Activity) {
            a = ((Activity) getContext());
        } else {
            Toast.makeText(getContext(), "Error", Toast.LENGTH_SHORT).show();
            return false;
        }
        Intent intent;
        switch (item.getItemId()) {
            case R.id.share:
                //share multiple items
                ArrayList<Uri> uris = new ArrayList<>();
                for (int i = 0; i < paths.length; i++) {
                    uris.add(StorageUtil
                            .getContentUriFromFilePath(getContext(), paths[i]));
                }

                intent = new Intent();
                intent.setAction(Intent.ACTION_SEND_MULTIPLE)
                        .setType(MediaType.getMimeType(getContext(), paths[0]))
                        .putExtra(Intent.EXTRA_STREAM, uris);

                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                if (intent.resolveActivity(getContext().getPackageManager()) != null) {
                    a.startActivity(Intent.createChooser(intent, getContext().getString(R.string.share_photo)));
                }
                break;
            case R.id.copy:
            case R.id.move:
                intent = new Intent(getContext(), FileOperationDialogActivity.class);
                intent.setAction(item.getItemId() == R.id.copy ?
                        FileOperationDialogActivity.ACTION_COPY :
                        FileOperationDialogActivity.ACTION_MOVE);
                intent.putExtra(FileOperationDialogActivity.FILES, paths);

                a.startActivityForResult(intent,
                        AlbumActivity.FILE_OP_DIALOG_REQUEST);
                break;
            case R.id.delete:
                Context c = getContext();

                String title = c.getString(R.string.delete) + " "
                        + String.valueOf(paths.length)
                        + " " + (paths.length > 1 ?
                        c.getString(R.string.files) : c.getString(R.string.file));

                new AlertDialog.Builder(c, MainActivity.getDialogThemeRes())
                        .setTitle(title)
                        .setNegativeButton(c.getString(R.string.no), null)
                        .setPositiveButton(c.getString(R.string.delete), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                deleteItems(paths);
                            }
                        })
                        .create().show();
                break;
        }

        return false;
    }

    private void cancelSelectorMode() {
        //cancel SelectorMode
        if (nestedRecyclerView.getAdapter() instanceof RecyclerViewAdapter) {
            ((RecyclerViewAdapter) nestedRecyclerView.getAdapter())
                    .cancelSelectorMode();
        }

        //update other ViewHolders
        final View rootView = ((Activity) nestedRecyclerView.getContext()).findViewById(R.id.root_view);
        View recyclerView = rootView.findViewById(R.id.recyclerView);
        if (recyclerView instanceof RecyclerView) {
            RecyclerView.Adapter adapter = ((RecyclerView) recyclerView).getAdapter();
            adapter.notifyItemRangeChanged(0, adapter.getItemCount() - 1);
        }
    }

    @Override
    public void onItemChanged() {
        nestedRecyclerView.getAdapter().notifyItemRangeChanged(0,
                nestedRecyclerView.getAdapter().getItemCount() - 1);
    }

    private void deleteItems(String[] paths) {
        File_POJO[] filesToDelete = new File_POJO[paths.length];
        for (int i = 0; i < filesToDelete.length; i++) {
            filesToDelete[i] = new File_POJO(paths[i], true);
        }

        final Activity a;
        if (getContext() instanceof Activity) {
            a = ((Activity) getContext());
        } else {
            Toast.makeText(getContext(), "Error", Toast.LENGTH_SHORT).show();
            return;
        }

        new Delete(filesToDelete)
                .execute(a, null,
                        new FileOperation.Callback() {
                            @Override
                            public void done() {
                                a.startActivity(new Intent(a, MainActivity.class)
                                        .setAction(MainActivity.REFRESH_MEDIA));
                            }

                            @Override
                            public void failed(String path) {

                            }
                        });
    }

    static class RecyclerViewAdapter
            extends us.koller.cameraroll.adapter.album.RecyclerViewAdapter {

        RecyclerViewAdapter(SelectorModeManager.Callback callback, final RecyclerView recyclerView,
                            Album album, boolean pick_photos) {
            super(callback, recyclerView, album, pick_photos);
        }

        @Override
        public boolean dragSelectEnabled() {
            return false;
        }
    }

    static class SelectorModeUtil {

        static final String SELECTOR_TOOLBAR_TAG = "SELECTOR_TOOLBAR_TAG";

        static Toolbar getSelectorModeToolbar(Context context,
                                              View.OnClickListener onClickListener,
                                              Toolbar.OnMenuItemClickListener onItemClickListener) {
            final Toolbar toolbar = new Toolbar(context);
            toolbar.setTag(SELECTOR_TOOLBAR_TAG);

            int accentColor = ContextCompat.getColor(context,
                    MainActivity.accent_color_res);
            int textColor = ContextCompat.getColor(context,
                    R.color.grey_900_translucent);

            toolbar.setBackgroundColor(accentColor);

            toolbar.setTitleTextColor(textColor);

            toolbar.inflateMenu(R.menu.selector_mode);
            toolbar.setOnMenuItemClickListener(onItemClickListener);

            Drawable menuIcon = toolbar.getOverflowIcon();
            if (menuIcon != null) {
                DrawableCompat.wrap(menuIcon);
                DrawableCompat.setTint(menuIcon.mutate(), textColor);
            }

            Drawable navIcon = ContextCompat.getDrawable(context,
                    R.drawable.ic_clear_black_24dp);
            DrawableCompat.wrap(navIcon);
            DrawableCompat.setTint(navIcon.mutate(), textColor);
            toolbar.setNavigationIcon(navIcon);

            toolbar.setNavigationOnClickListener(onClickListener);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                toolbar.setElevation(context.getResources()
                        .getDimension(R.dimen.toolbar_elevation));
            }
            return toolbar;
        }
    }
}