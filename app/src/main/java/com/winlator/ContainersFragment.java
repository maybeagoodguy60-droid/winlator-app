package com.winlator;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.winlator.container.Container;
import com.winlator.container.ContainerManager;
import com.winlator.contentdialog.ContentDialog;
import com.winlator.contentdialog.RootfsPickerDialog;
import com.winlator.contentdialog.StorageInfoDialog;
import com.winlator.core.DownloadProgressDialog;
import com.winlator.core.PreloaderDialog;
import com.winlator.linux.LaunchValidator;
import com.winlator.linux.LinuxContainer;
import com.winlator.linux.RootfsDownloader;
import com.winlator.xenvironment.RootFS;
import com.winlator.xenvironment.RootFSInstaller;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ContainersFragment extends Fragment {
    private RecyclerView recyclerView;
    private TextView emptyTextView;
    private ContainerManager manager;
    private PreloaderDialog preloaderDialog;
    private ActivityResultLauncher<Intent> importFileLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        preloaderDialog = new PreloaderDialog(getActivity());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        manager = new ContainerManager(getContext());
        importFileLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                importRootfs(result.getData().getData());
            }
        });
        loadContainersList();
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(R.string.containers);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FrameLayout frameLayout = (FrameLayout)inflater.inflate(R.layout.containers_fragment, container, false);
        recyclerView = frameLayout.findViewById(R.id.RecyclerView);
        Context context = recyclerView.getContext();
        emptyTextView = frameLayout.findViewById(R.id.TVEmptyText);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));

        DividerItemDecoration itemDecoration = new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);
        itemDecoration.setDrawable(ContextCompat.getDrawable(context, R.drawable.list_item_divider));
        recyclerView.addItemDecoration(itemDecoration);
        return frameLayout;
    }

    private void loadContainersList() {
        ArrayList<Container> containers = manager.getContainers();
        recyclerView.setAdapter(new ContainersAdapter(containers));
        if (containers.isEmpty()) {
            emptyTextView.setVisibility(View.VISIBLE);
            if (RootFS.find(getContext()).isValid()) {
                emptyTextView.setText(R.string.containers_empty_hint);
            } else {
                emptyTextView.setText(R.string.containers_empty_no_rootfs);
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.containers_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.menu_item_add) {
            showRootfsPickerOrCreate();
            return true;
        }
        else return super.onOptionsItemSelected(menuItem);
    }

    private void showRootfsPickerOrCreate() {
        if (RootFS.find(getContext()).isValid()) {
            openNewContainer("");
            return;
        }
        RootfsPickerDialog.show(getContext(), new RootfsPickerDialog.Callback() {
            @Override
            public void onDownload() {
                downloadRootfs();
            }

            @Override
            public void onImportFromFile() {
                Intent pickerIntent = new Intent(Intent.ACTION_GET_CONTENT);
                pickerIntent.setType("application/gzip");
                pickerIntent.addCategory(Intent.CATEGORY_OPENABLE);
                importFileLauncher.launch(pickerIntent);
            }

            @Override
            public void onUseExistingPath(String path) {
                useExistingRootfs(path);
            }
        });
    }

    private void openNewContainer(String rootfsPath) {
        openNewContainer();
    }

    private void useExistingRootfs(String path) {
        File rootDir = new File(path);
        if (!LaunchValidator.hasRootfs(rootDir)) {
            Toast.makeText(getContext(), R.string.rootfs_validate_fail, Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(getContext(), R.string.rootfs_ready, Toast.LENGTH_SHORT).show();
        openNewContainer(rootDir.getPath());
    }

    private void importRootfs(Uri uri) {
        final Activity activity = getActivity();
        if (activity == null) return;
        preloaderDialog.show(R.string.rootfs_importing);
        final File rootDir = RootFS.find(activity).getRootDir();
        new Thread(() -> {
            boolean success = RootFSInstaller.importFromUri(activity, uri, rootDir);
            activity.runOnUiThread(() -> {
                preloaderDialog.close();
                if (success) {
                    RootFS.find(activity).createRFSVersionFile(0);
                    Toast.makeText(activity, R.string.rootfs_installed, Toast.LENGTH_LONG).show();
                    loadContainersList();
                    openNewContainer(rootDir.getPath());
                } else {
                    Toast.makeText(activity, R.string.rootfs_import_failed, Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    private void downloadRootfs() {
        final Activity activity = getActivity();
        if (activity == null) return;
        final DownloadProgressDialog progressDialog = new DownloadProgressDialog(activity);
        progressDialog.show(R.string.downloading_rootfs);
        RootfsDownloader.downloadRootfs(activity, "debian", activity.getCacheDir().getAbsolutePath(), new RootfsDownloader.Callback() {
            @Override
            public void onProgress(int progress) {
                progressDialog.setProgress(progress);
            }

            @Override
            public void onComplete(boolean success, String message) {
                activity.runOnUiThread(() -> {
                    if (!success) {
                        progressDialog.close();
                        Toast.makeText(activity, getString(R.string.download_failed, message), Toast.LENGTH_LONG).show();
                        return;
                    }
                    extractDownloadedRootfs(activity, progressDialog, new File(message));
                });
            }
        });
    }

    private void extractDownloadedRootfs(Activity activity, DownloadProgressDialog progressDialog, File tarGzFile) {
        final File rootDir = RootFS.find(activity).getRootDir();
        new Thread(() -> {
            boolean success = RootFSInstaller.extractTarGz(tarGzFile, rootDir);
            activity.runOnUiThread(() -> {
                progressDialog.close();
                if (success) {
                    RootFS.find(activity).createRFSVersionFile(0);
                    Toast.makeText(activity, R.string.rootfs_installed, Toast.LENGTH_LONG).show();
                    loadContainersList();
                    openNewContainer(rootDir.getPath());
                } else {
                    Toast.makeText(activity, R.string.rootfs_import_failed, Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    private class ContainersAdapter extends RecyclerView.Adapter<ContainersAdapter.ViewHolder> {
        private final List<Container> data;

        private class ViewHolder extends RecyclerView.ViewHolder {
            private final ImageView runButton;
            private final ImageView menuButton;
            private final ImageView imageView;
            private final TextView title;
            private final TextView subtitle;

            private ViewHolder(View view) {
                super(view);
                this.imageView = view.findViewById(R.id.ImageView);
                this.title = view.findViewById(R.id.TVTitle);
                this.subtitle = view.findViewById(R.id.TVSubtitle);
                this.runButton = view.findViewById(R.id.BTRun);
                this.menuButton = view.findViewById(R.id.BTMenu);
            }
        }

        public ContainersAdapter(List<Container> data) {
            this.data = data;
        }

        @Override
        public final ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.container_list_item, parent, false));
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            final Container item = data.get(position);
            holder.imageView.setImageResource(R.drawable.icon_container);
            holder.title.setText(item.getName());
            holder.subtitle.setVisibility(View.VISIBLE);
            String status = buildStatus(item);
            holder.subtitle.setText(status);
            holder.subtitle.setTextColor(LaunchValidator.hasRootfs(effectiveRootDir(item))
                ? 0xFF66BB6A : 0xFFFF8A80);
            holder.runButton.setOnClickListener((view) -> runContainer(item));
            holder.menuButton.setOnClickListener((view) -> showListItemMenu(view, item));
        }

        @Override
        public final int getItemCount() {
            return data.size();
        }

        private void showListItemMenu(View anchorView, Container container) {
            MainActivity activity = (MainActivity)getActivity();
            PopupMenu listItemMenu = new PopupMenu(activity, anchorView);
            listItemMenu.inflate(R.menu.container_popup_menu);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) listItemMenu.setForceShowIcon(true);

            listItemMenu.setOnMenuItemClickListener((menuItem) -> {
                switch (menuItem.getItemId()) {
                    case R.id.menu_item_file_manager:
                        activity.showFragment(new ContainerFileManagerFragment(container.id));
                        break;
                    case R.id.menu_item_edit:
                        activity.showFragment(new ContainerDetailFragment(container.id));
                        break;
                    case R.id.menu_item_duplicate:
                        ContentDialog.confirm(getContext(), R.string.do_you_want_to_duplicate_this_container, () -> {
                            preloaderDialog.show(R.string.duplicating_container);
                            manager.duplicateContainerAsync(container, () -> {
                                preloaderDialog.close();
                                loadContainersList();
                            });
                        });
                        break;
                    case R.id.menu_item_remove:
                        ContentDialog.confirm(getContext(), R.string.do_you_want_to_remove_this_container, () -> {
                            preloaderDialog.show(R.string.removing_container);
                            manager.removeContainerAsync(container, () -> {
                                preloaderDialog.close();
                                loadContainersList();
                            });
                        });
                        break;
                    case R.id.menu_item_info:
                        (new StorageInfoDialog(activity, container)).show();
                        break;
                }
                return true;
            });
            listItemMenu.show();
        }

        private void runContainer(Container container) {
            MainActivity activity = (MainActivity)getActivity();
            if (container instanceof LinuxContainer && !LaunchValidator.hasRootfs(effectiveRootDir(container))) {
                Toast.makeText(activity, R.string.no_rootfs_found, Toast.LENGTH_LONG).show();
                activity.showFragment(new ContainerDetailFragment(container.id));
                return;
            }
            Intent intent = new Intent(activity, XServerDisplayActivity.class);
            intent.putExtra("container_id", container.id);
            activity.startActivity(intent);
        }

        private File effectiveRootDir(Container container) {
            if (container instanceof LinuxContainer) {
                LinuxContainer linuxContainer = (LinuxContainer)container;
                if (linuxContainer.hasRootfs()) return linuxContainer.getRootfsDir();
            }
            return RootFS.find(ContainersFragment.this.getContext()).getRootDir();
        }

        private String buildStatus(Container container) {
            StringBuilder status = new StringBuilder();
            boolean rootfsOk = LaunchValidator.hasRootfs(effectiveRootDir(container));
            if (container instanceof LinuxContainer) {
                LinuxContainer linuxContainer = (LinuxContainer)container;
                String mode;
                switch (linuxContainer.getLaunchMode()) {
                    case LinuxContainer.LAUNCH_MODE_CHROOT: mode = "Chroot"; break;
                    case LinuxContainer.LAUNCH_MODE_PROOT: mode = "Proot"; break;
                    default: mode = "Auto"; break;
                }
                status.insert(0, mode + " · " + linuxContainer.getDesktopEnv());
            }
            else {
                status.append("Wine");
            }
            return rootfsOk ? status.toString() : status.insert(0, "Rootfs missing · ").toString();
        }
    }
}
