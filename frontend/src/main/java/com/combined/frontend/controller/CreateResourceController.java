package com.combined.frontend.controller;

import com.combined.backend.dto.ResourceDto;
import com.combined.frontend.service.ApiService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.event.ActionEvent;

public class CreateResourceController {

    @FXML private TextField titleField;
    @FXML private TextField urlField;
    @FXML private TextField filePathField;
    @FXML private Label errorMessageLabel;

    private ApiService apiService;
    private Long groupId;
    private ResourceDto newResource;
    private ResourceDto resourceToEdit; // To hold the resource being edited
    private String filePath;

    public void setApiService(ApiService apiService) {
        this.apiService = apiService;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public void setResourceToEdit(ResourceDto resource) {
        this.resourceToEdit = resource;
        if (resource != null) {
            titleField.setText(resource.getTitle());
            urlField.setText(resource.getPathOrUrl());
            if ("FILE".equalsIgnoreCase(resource.getType())) {
                filePathField.setText(resource.getPathOrUrl());
                filePath = resource.getPathOrUrl();
            }
        }
    }

    public ResourceDto getNewResource() {
        return newResource;
    }

    @FXML
    private void initialize() {
        errorMessageLabel.setText("");
    }

    @FXML
    public void handleBrowseFile(javafx.event.ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select file to attach");
        Window window = filePathField.getScene() != null ? filePathField.getScene().getWindow() : null;
        java.io.File selected = chooser.showOpenDialog(window);
        if (selected != null) {
            filePath = selected.getAbsolutePath();
            filePathField.setText(filePath);
        }
    }

    public boolean processResult() { // For creating a new resource
        return saveResource(null);
    }

    public boolean processEditResult(Long resourceId) { // For editing an existing resource
        return saveResource(resourceId);
    }

    private boolean saveResource(Long resourceId) {
        String title = titleField.getText();
        String url = urlField.getText();

        if (title == null || title.trim().isEmpty()) {
            errorMessageLabel.setText("Resource title cannot be empty.");
            return false;
        }
        boolean hasFile = filePath != null && !filePath.isBlank();
        boolean hasUrl = url != null && !url.trim().isEmpty();

        if (!hasFile && !hasUrl) {
            errorMessageLabel.setText("Provide a URL or choose a file.");
            return false;
        }
        // Basic URL validation when provided
        if (hasUrl && !url.matches("^(http|https)://.*$")) {
            errorMessageLabel.setText("URL must start with http:// or https://");
            return false;
        }

        try {
            ResourceDto resourceToSave = new ResourceDto();
            resourceToSave.setGroupId(groupId);
            resourceToSave.setTitle(title);
            // Prefer file if chosen; otherwise treat as link
            if (hasFile) {
                resourceToSave.setPathOrUrl(filePath);
                resourceToSave.setType("FILE");
            } else {
                resourceToSave.setPathOrUrl(url);
                resourceToSave.setType("LINK");
            }

            if (resourceId == null) { // Create
                newResource = apiService.createResource(resourceToSave);
            } else { // Update
                newResource = apiService.updateResource(resourceId, resourceToSave);
            }
            return true;
        } catch (Exception e) {
            errorMessageLabel.setText("Failed to save resource: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
