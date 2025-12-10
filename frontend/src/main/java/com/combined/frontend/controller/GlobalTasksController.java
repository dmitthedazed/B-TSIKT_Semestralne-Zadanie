package com.combined.frontend.controller;

import com.combined.backend.dto.GroupDto;
import com.combined.backend.dto.TaskDto;
import com.combined.frontend.service.ApiService;
import com.combined.frontend.util.ErrorUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;
import javafx.event.ActionEvent;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class GlobalTasksController implements BaseController {

    @FXML private ComboBox<String> statusFilterComboBox;
    @FXML private ComboBox<String> participationComboBox;
    @FXML private ComboBox<GroupFilterItem> groupFilterComboBox;
    @FXML private TextField searchField;
    @FXML private TableView<TaskDto> tasksTable;
    @FXML private TableColumn<TaskDto, String> groupColumn;
    @FXML private TableColumn<TaskDto, String> titleColumn;
    @FXML private TableColumn<TaskDto, String> statusColumn;
    @FXML private TableColumn<TaskDto, String> assigneeColumn;
    @FXML private TableColumn<TaskDto, String> deadlineColumn;
    @FXML private TableColumn<TaskDto, String> createdByColumn;
    @FXML private TableColumn<TaskDto, Void> actionsColumn;
    @FXML private Label statusLabel;

    private final ObservableList<TaskDto> tasks = FXCollections.observableArrayList();
    private ApiService apiService;
    private Long currentUserId;
    private String currentNickname;
    private MainController mainController;
    private boolean dataInitialized = false;

    @Override
    public void setApiService(ApiService apiService) {
        this.apiService = apiService;
        tryInitData();
    }

    @Override
    public void setCurrentUser(Long userId, String nickname) {
        this.currentUserId = userId;
        this.currentNickname = nickname;
        tryInitData();
    }

    @Override
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    private void initialize() {
        setupFilters();
        setupTable();
    }

    @FXML
    private void onRefreshClick() {
        loadTasks();
    }

    private void setupFilters() {
        statusFilterComboBox.setItems(FXCollections.observableArrayList("All", "OPEN", "IN_PROGRESS", "DONE"));
        statusFilterComboBox.getSelectionModel().selectFirst();
        participationComboBox.setItems(FXCollections.observableArrayList("ALL", "ASSIGNED", "CREATED"));
        participationComboBox.getSelectionModel().selectFirst();

        groupFilterComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(GroupFilterItem object) {
                return object == null ? "" : object.getLabel();
            }

            @Override
            public GroupFilterItem fromString(String string) {
                return null;
            }
        });

        // React to filter changes
        statusFilterComboBox.valueProperty().addListener((obs, o, n) -> loadTasks());
        participationComboBox.valueProperty().addListener((obs, o, n) -> loadTasks());
        groupFilterComboBox.valueProperty().addListener((obs, o, n) -> loadTasks());
        searchField.textProperty().addListener((obs, o, n) -> loadTasks());
    }

    private void setupTable() {
        groupColumn.setCellValueFactory(new PropertyValueFactory<>("groupName"));
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        assigneeColumn.setCellValueFactory(new PropertyValueFactory<>("assignedToName"));
        deadlineColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getDeadline() == null) {
                return new javafx.beans.property.SimpleStringProperty("");
            }
            return new javafx.beans.property.SimpleStringProperty(
                    cellData.getValue().getDeadline().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            );
        });
        createdByColumn.setCellValueFactory(new PropertyValueFactory<>("createdByName"));

        tasksTable.setItems(tasks);
        tasksTable.setRowFactory(tv -> {
            TableRow<TaskDto> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openGroupForTask(row.getItem());
                }
            });
            return row;
        });

        addStatusCellFactory();
        addActionsColumn();
    }

    private void addStatusCellFactory() {
        statusColumn.setCellFactory(col -> new TableCell<>() {
            private final ComboBox<String> combo = new ComboBox<>(FXCollections.observableArrayList("OPEN", "IN_PROGRESS", "DONE"));

            {
                combo.setMaxWidth(Double.MAX_VALUE);
                combo.valueProperty().addListener((obs, oldVal, newVal) -> {
                    TaskDto task = getTableRow().getItem();
                    if (task != null && newVal != null && !newVal.equals(task.getStatus())) {
                        updateStatus(task, newVal);
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    combo.getSelectionModel().select(getTableRow().getItem().getStatus());
                    setGraphic(combo);
                }
            }
        });
    }

    private void addActionsColumn() {
        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button openButton = new Button("Open group");
            private final Button editButton = new Button("Edit");
            private final HBox box = new HBox(6, openButton, editButton);

            {
                openButton.setOnAction(e -> {
                    TaskDto task = getTableRow().getItem();
                    if (task != null) openGroupForTask(task);
                });
                editButton.setOnAction(e -> {
                    TaskDto task = getTableRow().getItem();
                    if (task != null) openEditDialog(task);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void loadTasks() {
        if (apiService == null || currentUserId == null) {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("User not set.");
            return;
        }

        statusLabel.setStyle("-fx-text-fill: #555;");
        statusLabel.setText("Loading tasks…");

        String statusFilter = statusFilterComboBox.getValue();
        String participation = participationComboBox.getValue();
        GroupFilterItem selectedGroup = groupFilterComboBox.getSelectionModel().getSelectedItem();
        Long groupId = selectedGroup != null ? selectedGroup.getGroupId() : null;
        String query = searchField.getText();

        new Thread(() -> {
            try {
                List<TaskDto> loaded = apiService.getMyTasks(
                        "All".equalsIgnoreCase(statusFilter) ? null : statusFilter,
                        groupId,
                        participation,
                        query
                );
                // keep table sorted by deadline then title
                loaded = loaded.stream()
                        .sorted(Comparator.comparing(TaskDto::getDeadline, Comparator.nullsLast(Comparator.naturalOrder()))
                                .thenComparing(TaskDto::getTitle, Comparator.nullsLast(String::compareToIgnoreCase)))
                        .collect(Collectors.toList());
                List<TaskDto> finalLoaded = loaded;
                Platform.runLater(() -> {
                    tasks.setAll(finalLoaded);
                    statusLabel.setStyle("-fx-text-fill: green;");
                    statusLabel.setText("Loaded " + finalLoaded.size() + " tasks.");
                });
            } catch (Exception e) {
                String reason = ErrorUtils.extractReason(e);
                if (!reason.isBlank() && reason.toLowerCase().contains("unauthorized")) {
                    if (mainController != null) mainController.handleUnauthorized("tasks");
                    return;
                }
                Platform.runLater(() -> {
                    statusLabel.setStyle("-fx-text-fill: red;");
                    statusLabel.setText(ErrorUtils.formatError("Error loading tasks", e));
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void updateStatus(TaskDto task, String newStatus) {
        TaskDto payload = new TaskDto();
        payload.setGroupId(task.getGroupId());
        payload.setTitle(task.getTitle());
        payload.setDescription(task.getDescription());
        payload.setStatus(newStatus);
        payload.setDeadline(task.getDeadline());
        payload.setAssignedToId(task.getAssignedToId());

        new Thread(() -> {
            try {
                TaskDto updated = apiService.updateTask(task.getId(), payload);
                Platform.runLater(() -> {
                    task.setStatus(updated.getStatus());
                    tasksTable.refresh();
                });
            } catch (Exception e) {
                String reason = ErrorUtils.extractReason(e);
                if (!reason.isBlank() && reason.toLowerCase().contains("unauthorized")) {
                    if (mainController != null) mainController.handleUnauthorized("tasks");
                    return;
                }
                Platform.runLater(() -> {
                    tasksTable.refresh();
                    showAlert(Alert.AlertType.ERROR, "Update failed", ErrorUtils.formatError("Could not update status", e));
                });
            }
        }).start();
    }

    private void openGroupForTask(TaskDto task) {
        if (mainController == null) return;
        new Thread(() -> {
            try {
                GroupDto group = apiService.getGroupById(task.getGroupId());
                Platform.runLater(() -> mainController.loadGroupDetailView(group));
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Open group", ErrorUtils.formatError("Failed to open group", e)));
            }
        }).start();
    }

    private void openEditDialog(TaskDto task) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/create-task-dialog.fxml"));
            DialogPane pane = loader.load();
            CreateTaskController controller = loader.getController();
            controller.setApiService(apiService);
            controller.setGroupId(task.getGroupId());
            controller.setTaskToEdit(task);
            controller.setMainController(mainController);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(pane);
            dialog.setTitle("Edit task: " + task.getTitle());

            Button okButton = (Button) pane.lookupButton(ButtonType.OK);
            okButton.addEventFilter(ActionEvent.ACTION, evt -> {
                boolean ok = controller.processEditResult(task.getId());
                if (!ok) {
                    evt.consume();
                }
            });

            dialog.showAndWait();
            if (controller.wasSaved()) {
                loadTasks();
            } else {
                String reason = controller.getLastErrorMessage();
                if (reason != null && !reason.isBlank()) {
                    showAlert(Alert.AlertType.ERROR, "Edit task", reason);
                }
            }
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Edit task", ErrorUtils.formatError("Could not open edit dialog", e));
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Called by MainController after the controller is fully configured.
     */
    @FXML
    private void initializeLater() {
        // no-op placeholder; kept for clarity if needed
    }

    /**
     * Utility holder for group filter items.
     */
    public static class GroupFilterItem {
        private final Long groupId;
        private final String label;

        public GroupFilterItem(Long groupId, String label) {
            this.groupId = groupId;
            this.label = label;
        }

        public Long getGroupId() {
            return groupId;
        }

        public String getLabel() {
            return label;
        }
    }

    /**
     * Loads groups into the filter once the ApiService/user are set.
     */
    private void loadGroupFilter() {
        if (apiService == null || currentUserId == null) return;
        new Thread(() -> {
            try {
                List<GroupDto> groups = apiService.getMyGroups();
                List<GroupFilterItem> items = groups.stream()
                        .map(g -> new GroupFilterItem(g.getId(), g.getName()))
                        .sorted(Comparator.comparing(GroupFilterItem::getLabel, String.CASE_INSENSITIVE_ORDER))
                        .collect(Collectors.toList());
                Platform.runLater(() -> {
                    groupFilterComboBox.getItems().clear();
                    groupFilterComboBox.getItems().add(new GroupFilterItem(null, "All groups"));
                    groupFilterComboBox.getItems().addAll(items);
                    groupFilterComboBox.getSelectionModel().selectFirst();
                });
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText(ErrorUtils.formatError("Failed to load groups", e)));
            }
        }).start();
    }

    /**
     * MainController calls this after wiring dependencies to trigger the initial data load.
     */
    @FXML
    public void initializeData() {
        loadGroupFilter();
        loadTasks();
    }

    private void tryInitData() {
        if (!dataInitialized && apiService != null && currentUserId != null) {
            dataInitialized = true;
            loadGroupFilter();
            loadTasks();
        }
    }
}
