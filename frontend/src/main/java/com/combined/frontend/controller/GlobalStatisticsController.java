package com.combined.frontend.controller;

import com.combined.backend.dto.ActivityLogDto;
import com.combined.backend.dto.TaskDto;
import com.combined.frontend.service.ApiService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class GlobalStatisticsController implements BaseController {

    // Filters
    @FXML private ComboBox<String> periodComboBox;
    @FXML private ComboBox<String> scopeComboBox;

    // Summary labels
    @FXML private Label totalActionsLabel;
    @FXML private Label tasksCreatedLabel;
    @FXML private Label tasksDoneLabel;
    @FXML private Label groupsActiveLabel;

    // My Activity
    @FXML private LineChart<String, Number> activityLineChart;
    @FXML private TableView<GroupActivityRow> topGroupsTable;
    @FXML private TableColumn<GroupActivityRow, String> topGroupNameCol;
    @FXML private TableColumn<GroupActivityRow, Integer> topGroupActionsCol;
    @FXML private TableColumn<GroupActivityRow, Integer> topGroupTasksCreatedCol;
    @FXML private TableColumn<GroupActivityRow, Integer> topGroupTasksDoneCol;

    // Tasks Overview
    @FXML private PieChart statusPieChart;
    @FXML private BarChart<String, Number> tasksByGroupBarChart;
    @FXML private TableView<TaskRow> overdueTable;
    @FXML private TableColumn<TaskRow, String> overdueGroupCol;
    @FXML private TableColumn<TaskRow, String> overdueTitleCol;
    @FXML private TableColumn<TaskRow, String> overdueDeadlineCol;
    @FXML private TableColumn<TaskRow, String> overdueStatusCol;
    @FXML private TableColumn<TaskRow, String> overdueAssigneeCol;

    // Groups Overview
    @FXML private TableView<GroupSummaryRow> groupsTable;
    @FXML private TableColumn<GroupSummaryRow, String> groupNameCol;
    @FXML private TableColumn<GroupSummaryRow, Integer> groupTotalCol;
    @FXML private TableColumn<GroupSummaryRow, Integer> groupOpenCol;
    @FXML private TableColumn<GroupSummaryRow, Integer> groupInProgressCol;
    @FXML private TableColumn<GroupSummaryRow, Integer> groupDoneCol;
    @FXML private TableColumn<GroupSummaryRow, Integer> groupMyActionsCol;
    @FXML private TableColumn<GroupSummaryRow, String> groupLastActivityCol;
    @FXML private BarChart<String, Number> activityPerGroupBarChart;

    @FXML private Label statusLabel;

    private ApiService apiService;
    private Long currentUserId;
    private String currentNickname;
    private MainController mainController;

    private List<TaskDto> cachedTasks;
    private List<ActivityLogDto> cachedLogs;

    @Override
    public void setApiService(ApiService apiService) {
        this.apiService = apiService;
        triggerLoad();
    }

    @Override
    public void setCurrentUser(Long userId, String nickname) {
        this.currentUserId = userId;
        this.currentNickname = nickname;
        triggerLoad();
    }

    @Override
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    private void initialize() {
        periodComboBox.setItems(FXCollections.observableArrayList("Last 7 days", "Last 30 days", "Last 90 days", "All time"));
        periodComboBox.getSelectionModel().select(1); // default 30 days
        scopeComboBox.setItems(FXCollections.observableArrayList("All tasks in my groups", "Only tasks assigned to me"));
        scopeComboBox.getSelectionModel().selectFirst();

        periodComboBox.valueProperty().addListener((obs, o, n) -> triggerLoad());
        scopeComboBox.valueProperty().addListener((obs, o, n) -> updateCharts());

        topGroupNameCol.setCellValueFactory(new PropertyValueFactory<>("groupName"));
        topGroupActionsCol.setCellValueFactory(new PropertyValueFactory<>("actions"));
        topGroupTasksCreatedCol.setCellValueFactory(new PropertyValueFactory<>("tasksCreated"));
        topGroupTasksDoneCol.setCellValueFactory(new PropertyValueFactory<>("tasksDone"));

        overdueGroupCol.setCellValueFactory(new PropertyValueFactory<>("groupName"));
        overdueTitleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        overdueDeadlineCol.setCellValueFactory(new PropertyValueFactory<>("deadline"));
        overdueStatusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        overdueAssigneeCol.setCellValueFactory(new PropertyValueFactory<>("assignee"));

        groupNameCol.setCellValueFactory(new PropertyValueFactory<>("groupName"));
        groupTotalCol.setCellValueFactory(new PropertyValueFactory<>("total"));
        groupOpenCol.setCellValueFactory(new PropertyValueFactory<>("open"));
        groupInProgressCol.setCellValueFactory(new PropertyValueFactory<>("inProgress"));
        groupDoneCol.setCellValueFactory(new PropertyValueFactory<>("done"));
        groupMyActionsCol.setCellValueFactory(new PropertyValueFactory<>("myActions"));
        groupLastActivityCol.setCellValueFactory(new PropertyValueFactory<>("lastActivity"));
    }

    private void triggerLoad() {
        if (apiService == null || currentUserId == null) return;
        loadData();
    }

    private void loadData() {
        statusLabel.setText("Loading statistics...");
        Integer days = periodToDays(periodComboBox.getValue());

        new Thread(() -> {
            try {
                cachedTasks = apiService.getMyTasks(currentUserId, null, null, "ALL", null);
                cachedLogs = apiService.getUserActivityLogs(currentUserId, days);
                Platform.runLater(() -> {
                    updateCharts();
                    statusLabel.setText("Statistics loaded.");
                });
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Failed to load statistics: " + e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    private void updateCharts() {
        if (cachedTasks == null) return;
        Integer days = periodToDays(periodComboBox.getValue());
        List<TaskDto> tasks = filterTasksByPeriod(cachedTasks, days);
        List<ActivityLogDto> logs = filterLogsByPeriod(cachedLogs, days);

        boolean onlyAssigned = scopeComboBox.getSelectionModel().getSelectedIndex() == 1;
        if (onlyAssigned) {
            tasks = tasks.stream()
                    .filter(t -> t.getAssignedToId() != null && Objects.equals(t.getAssignedToId(), currentUserId))
                    .collect(Collectors.toList());
        }

        // Summary
        long totalActions = logs != null ? logs.size() : 0;
        long tasksCreated = tasks.stream().filter(t -> Objects.equals(t.getCreatedById(), currentUserId)).count();
        long tasksDone = tasks.stream().filter(t -> "DONE".equalsIgnoreCase(t.getStatus()) && Objects.equals(t.getAssignedToId(), currentUserId)).count();
        long groupsActive = logs == null ? 0 : logs.stream().map(ActivityLogDto::getGroupId).filter(Objects::nonNull).distinct().count();

        totalActionsLabel.setText(String.valueOf(totalActions));
        tasksCreatedLabel.setText(String.valueOf(tasksCreated));
        tasksDoneLabel.setText(String.valueOf(tasksDone));
        groupsActiveLabel.setText(String.valueOf(groupsActive));

        updateActivityChart(logs);
        updateTopGroups(logs, tasks);
        updateStatusPie(tasks);
        updateTasksByGroup(tasks);
        updateOverdue(tasks);
        updateGroupSummary(tasks, logs);
    }

    private void updateActivityChart(List<ActivityLogDto> logs) {
        activityLineChart.getData().clear();
        if (logs == null || logs.isEmpty()) return;

        Map<LocalDate, Long> perDay = logs.stream()
                .collect(Collectors.groupingBy(l -> l.getTimestamp().toLocalDate(), Collectors.counting()));
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        perDay.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> series.getData().add(new XYChart.Data<>(e.getKey().toString(), e.getValue())));
        activityLineChart.getData().add(series);
    }

    private void updateTopGroups(List<ActivityLogDto> logs, List<TaskDto> tasks) {
        ObservableList<GroupActivityRow> rows = FXCollections.observableArrayList();
        Map<Long, Long> actionsByGroup = logs == null ? Map.of() :
                logs.stream()
                        .filter(l -> l.getGroupId() != null)
                        .collect(Collectors.groupingBy(ActivityLogDto::getGroupId, Collectors.counting()));

        Map<Long, Long> tasksCreated = tasks.stream()
                .collect(Collectors.groupingBy(TaskDto::getGroupId,
                        Collectors.filtering(t -> Objects.equals(t.getCreatedById(), currentUserId), Collectors.counting())));
        Map<Long, Long> tasksDone = tasks.stream()
                .collect(Collectors.groupingBy(TaskDto::getGroupId,
                        Collectors.filtering(t -> "DONE".equalsIgnoreCase(t.getStatus()) && Objects.equals(t.getAssignedToId(), currentUserId), Collectors.counting())));

        actionsByGroup.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(10)
                .forEach(e -> {
                    Long gid = e.getKey();
                    String name = tasks.stream().filter(t -> Objects.equals(t.getGroupId(), gid)).findFirst().map(TaskDto::getGroupName).orElse("Group " + gid);
                    rows.add(new GroupActivityRow(name,
                            e.getValue().intValue(),
                            tasksCreated.getOrDefault(gid, 0L).intValue(),
                            tasksDone.getOrDefault(gid, 0L).intValue()));
                });
        topGroupsTable.setItems(rows);
    }

    private void updateStatusPie(List<TaskDto> tasks) {
        statusPieChart.getData().clear();
        Map<String, Long> counts = tasks.stream()
                .collect(Collectors.groupingBy(t -> t.getStatus() == null ? "UNKNOWN" : t.getStatus().toUpperCase(), Collectors.counting()));
        counts.forEach((status, count) -> statusPieChart.getData().add(new PieChart.Data(status + " (" + count + ")", count)));
    }

    private void updateTasksByGroup(List<TaskDto> tasks) {
        tasksByGroupBarChart.getData().clear();
        Map<String, Long> counts = tasks.stream()
                .collect(Collectors.groupingBy(TaskDto::getGroupName, Collectors.counting()));
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(e -> series.getData().add(new XYChart.Data<>(e.getKey(), e.getValue())));
        tasksByGroupBarChart.getData().add(series);
    }

    private void updateOverdue(List<TaskDto> tasks) {
        ObservableList<TaskRow> rows = FXCollections.observableArrayList();
        LocalDateTime now = LocalDateTime.now();
        tasks.stream()
                .filter(t -> t.getDeadline() != null && t.getDeadline().isBefore(now))
                .filter(t -> !"DONE".equalsIgnoreCase(t.getStatus()))
                .sorted(Comparator.comparing(TaskDto::getDeadline))
                .forEach(t -> rows.add(new TaskRow(
                        t.getGroupName(),
                        t.getTitle(),
                        t.getDeadline().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                        t.getStatus(),
                        t.getAssignedToName() == null ? "Unassigned" : t.getAssignedToName()
                )));
        overdueTable.setItems(rows);
    }

    private void updateGroupSummary(List<TaskDto> tasks, List<ActivityLogDto> logs) {
        ObservableList<GroupSummaryRow> rows = FXCollections.observableArrayList();
        Map<Long, List<TaskDto>> byGroup = tasks.stream().collect(Collectors.groupingBy(TaskDto::getGroupId));
        Map<Long, Long> actions = logs == null ? new HashMap<>() :
                logs.stream().filter(l -> l.getGroupId() != null)
                        .collect(Collectors.groupingBy(ActivityLogDto::getGroupId, Collectors.counting()));
        Map<Long, LocalDateTime> lastActivity = logs == null ? new HashMap<>() :
                logs.stream().filter(l -> l.getGroupId() != null)
                        .collect(Collectors.toMap(ActivityLogDto::getGroupId, ActivityLogDto::getTimestamp,
                                (a, b) -> a.isAfter(b) ? a : b));

        byGroup.forEach((gid, list) -> {
            String name = list.get(0).getGroupName();
            long total = list.size();
            long open = list.stream().filter(t -> "OPEN".equalsIgnoreCase(t.getStatus())).count();
            long inProg = list.stream().filter(t -> "IN_PROGRESS".equalsIgnoreCase(t.getStatus())).count();
            long done = list.stream().filter(t -> "DONE".equalsIgnoreCase(t.getStatus())).count();
            int myActions = actions.getOrDefault(gid, 0L).intValue();
            String last = lastActivity.containsKey(gid)
                    ? lastActivity.get(gid).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                    : "—";
            rows.add(new GroupSummaryRow(name, (int) total, (int) open, (int) inProg, (int) done, myActions, last));
        });
        groupsTable.setItems(rows);

        activityPerGroupBarChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        rows.stream()
                .sorted(Comparator.comparing(GroupSummaryRow::getMyActions).reversed())
                .forEach(r -> series.getData().add(new XYChart.Data<>(r.getGroupName(), r.getMyActions())));
        activityPerGroupBarChart.getData().add(series);
    }

    private Integer periodToDays(String period) {
        if (period == null) return 30;
        switch (period) {
            case "Last 7 days": return 7;
            case "Last 30 days": return 30;
            case "Last 90 days": return 90;
            default: return null; // All time
        }
    }

    private List<TaskDto> filterTasksByPeriod(List<TaskDto> tasks, Integer days) {
        if (days == null) return tasks;
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        return tasks.stream()
                .filter(t -> t.getCreatedAt() == null || !t.getCreatedAt().isBefore(cutoff))
                .collect(Collectors.toList());
    }

    private List<ActivityLogDto> filterLogsByPeriod(List<ActivityLogDto> logs, Integer days) {
        if (logs == null) return List.of();
        if (days == null) return logs;
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        return logs.stream()
                .filter(l -> !l.getTimestamp().isBefore(cutoff))
                .collect(Collectors.toList());
    }

    // Row models
    public static class GroupActivityRow {
        private final String groupName;
        private final int actions;
        private final int tasksCreated;
        private final int tasksDone;

        public GroupActivityRow(String groupName, int actions, int tasksCreated, int tasksDone) {
            this.groupName = groupName;
            this.actions = actions;
            this.tasksCreated = tasksCreated;
            this.tasksDone = tasksDone;
        }

        public String getGroupName() {
            return groupName;
        }

        public int getActions() {
            return actions;
        }

        public int getTasksCreated() {
            return tasksCreated;
        }

        public int getTasksDone() {
            return tasksDone;
        }
    }

    public static class TaskRow {
        private final String groupName;
        private final String title;
        private final String deadline;
        private final String status;
        private final String assignee;

        public TaskRow(String groupName, String title, String deadline, String status, String assignee) {
            this.groupName = groupName;
            this.title = title;
            this.deadline = deadline;
            this.status = status;
            this.assignee = assignee;
        }

        public String getGroupName() {
            return groupName;
        }

        public String getTitle() {
            return title;
        }

        public String getDeadline() {
            return deadline;
        }

        public String getStatus() {
            return status;
        }

        public String getAssignee() {
            return assignee;
        }
    }

    public static class GroupSummaryRow {
        private final String groupName;
        private final int total;
        private final int open;
        private final int inProgress;
        private final int done;
        private final int myActions;
        private final String lastActivity;

        public GroupSummaryRow(String groupName, int total, int open, int inProgress, int done, int myActions, String lastActivity) {
            this.groupName = groupName;
            this.total = total;
            this.open = open;
            this.inProgress = inProgress;
            this.done = done;
            this.myActions = myActions;
            this.lastActivity = lastActivity;
        }

        public String getGroupName() {
            return groupName;
        }

        public int getTotal() {
            return total;
        }

        public int getOpen() {
            return open;
        }

        public int getInProgress() {
            return inProgress;
        }

        public int getDone() {
            return done;
        }

        public int getMyActions() {
            return myActions;
        }

        public String getLastActivity() {
            return lastActivity;
        }
    }
}
