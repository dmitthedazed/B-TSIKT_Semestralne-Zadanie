package com.combined.frontend.controller;

import com.combined.backend.dto.GroupDto;
import com.combined.backend.dto.TaskDto;
import com.combined.frontend.service.ApiService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CalendarController implements BaseController {

    @FXML private Label monthYearLabel;
    @FXML private GridPane calendarGrid;
    @FXML private Label statusLabel;

    private ApiService apiService;
    private Long currentUserId;
    private String currentNickname;
    private MainController mainController;
    private YearMonth currentMonth;

    @Override // Implement BaseController method
    public void setApiService(ApiService apiService) {
        this.apiService = apiService;
    }

    @Override // Implement BaseController method
    public void setCurrentUser(Long userId, String nickname) {
        this.currentUserId = userId;
        this.currentNickname = nickname;
    }

    @Override // Implement BaseController method
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    private void initialize() {
        currentMonth = YearMonth.now();
        drawCalendar();
    }

    @FXML
    private void handlePreviousMonth() {
        currentMonth = currentMonth.minusMonths(1);
        drawCalendar();
    }

    @FXML
    private void handleNextMonth() {
        currentMonth = currentMonth.plusMonths(1);
        drawCalendar();
    }

    private void drawCalendar() {
        monthYearLabel.setText(currentMonth.getMonth().name() + " " + currentMonth.getYear());

        // Clear previous calendar days (starting from row 1, as row 0 is for weekdays)
        // For simplicity, let's clear all children and re-add headers
        calendarGrid.getChildren().clear();
        addWeekdayHeaders(); // Re-add headers

        LocalDate calendarDate = LocalDate.of(currentMonth.getYear(), currentMonth.getMonth(), 1);
        int dayOfWeek = calendarDate.getDayOfWeek().getValue(); // 1 for Monday, 7 for Sunday
        int startColumn = (dayOfWeek == 7) ? 0 : dayOfWeek; // Adjust for Sunday being 0 in JavaFX GridPane

        int row = 1;
        for (int i = 0; i < startColumn; i++) {
            // Add empty cells for days before the 1st of the month
            calendarGrid.add(new VBox(), i, row);
        }

        while (calendarDate.getMonth() == currentMonth.getMonth()) {
            VBox dayCell = createDayCell(calendarDate);
            calendarGrid.add(dayCell, calendarDate.getDayOfWeek().getValue() % 7, row);

            calendarDate = calendarDate.plusDays(1);
            if (calendarDate.getDayOfWeek().getValue() == 1) { // If it's Monday, move to next row
                row++;
            }
        }

        loadTasksForCalendar();
    }

    private void addWeekdayHeaders() {
        String[] weekdays = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (int i = 0; i < weekdays.length; i++) {
            Label header = new Label(weekdays[i]);
            header.setStyle("-fx-font-weight: bold;");
            calendarGrid.add(header, i, 0);
        }
    }

    private VBox createDayCell(LocalDate date) {
        VBox cell = new VBox();
        cell.getStyleClass().add("calendar-day-cell");
        if (date.isEqual(LocalDate.now())) {
            cell.getStyleClass().add("current-day");
        }
        Label dayLabel = new Label(String.valueOf(date.getDayOfMonth()));
        dayLabel.getStyleClass().add("calendar-day-number");
        cell.getChildren().add(dayLabel);
        return cell;
    }

    // Changed from private to public
    public void loadTasksForCalendar() {
        if (apiService == null || currentUserId == null) {
            statusLabel.setText("User not logged in or ApiService not set.");
            return;
        }

        statusLabel.setText("Loading tasks for calendar...");

        new Thread(() -> {
            try {
                // Fetch all groups the user is a member of
                List<GroupDto> groups = apiService.getMyGroups();
                // Then fetch tasks for each group
                Map<LocalDate, List<TaskDto>> tasksByDate = new java.util.HashMap<>();
                for (GroupDto group : groups) {
                    List<TaskDto> tasks = apiService.getTasksByGroup(group.getId());
                    tasks.stream()
                            .filter(task -> task.getDeadline() != null &&
                                    YearMonth.from(task.getDeadline()).equals(currentMonth))
                            .forEach(task -> tasksByDate.computeIfAbsent(task.getDeadline().toLocalDate(), k -> new java.util.ArrayList<>()).add(task));
                }

                Platform.runLater(() -> {
                    addTasksToCalendar(tasksByDate);
                    statusLabel.setText("Tasks loaded for calendar.");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Error loading tasks for calendar: " + e.getMessage());
                });
            }
        }).start();
    }

    private void addTasksToCalendar(Map<LocalDate, List<TaskDto>> tasksByDate) {
        // Clear existing tasks from cells before adding new ones
        for (int row = 1; row < calendarGrid.getRowCount(); row++) {
            for (int col = 0; col < calendarGrid.getColumnCount(); col++) {
                // Check if the cell exists and has children beyond the day number
                if (calendarGrid.getChildren().size() > (row * calendarGrid.getColumnCount() + col) &&
                        calendarGrid.getChildren().get(row * calendarGrid.getColumnCount() + col) instanceof VBox) {
                    VBox cell = (VBox) calendarGrid.getChildren().get(row * calendarGrid.getColumnCount() + col);
                    if (cell.getChildren().size() > 1) { // Keep day number label
                        cell.getChildren().remove(1, cell.getChildren().size());
                    }
                }
            }
        }

        // Add new tasks
        for (int row = 1; row < calendarGrid.getRowCount(); row++) {
            for (int col = 0; col < calendarGrid.getColumnCount(); col++) {
                LocalDate dateInCell = getDateFromGridCell(row, col);

                if (dateInCell != null && tasksByDate.containsKey(dateInCell)) {
                    // Check if the cell exists and is a VBox
                    if (calendarGrid.getChildren().size() > (row * calendarGrid.getColumnCount() + col) &&
                            calendarGrid.getChildren().get(row * calendarGrid.getColumnCount() + col) instanceof VBox) {
                        VBox cell = (VBox) calendarGrid.getChildren().get(row * calendarGrid.getColumnCount() + col);
                        for (TaskDto task : tasksByDate.get(dateInCell)) {
                            Label taskLabel = new Label(task.getTitle());
                            taskLabel.getStyleClass().add("calendar-task-label");
                            cell.getChildren().add(taskLabel);
                        }
                    }
                }
            }
        }
    }

    // Helper method to get the date from a grid cell (simplified, might need refinement)
    private LocalDate getDateFromGridCell(int row, int col) {
        // This is a simplified approach. A more robust solution would involve
        // storing the date directly in the VBox's user data when creating the cell.
        // For now, we'll try to deduce it.
        int dayOfWeekOfFirstDay = LocalDate.of(currentMonth.getYear(), currentMonth.getMonth(), 1).getDayOfWeek().getValue();
        int startColumn = (dayOfWeekOfFirstDay == 7) ? 0 : dayOfWeekOfFirstDay; // 0 for Sunday, 1 for Monday

        // Calculate the day number in the month
        int dayNum = (row - 1) * 7 + col - startColumn + 1;

        if (dayNum > 0 && dayNum <= currentMonth.lengthOfMonth()) {
            return LocalDate.of(currentMonth.getYear(), currentMonth.getMonth(), dayNum);
        }
        return null;
    }
}
