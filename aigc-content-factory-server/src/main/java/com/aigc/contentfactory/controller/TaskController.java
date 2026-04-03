package com.aigc.contentfactory.controller;

import com.aigc.contentfactory.common.ApiResponse;
import com.aigc.contentfactory.dto.CreateTaskRequest;
import com.aigc.contentfactory.dto.PublishRequest;
import com.aigc.contentfactory.dto.ReviewActionRequest;
import com.aigc.contentfactory.dto.TaskDetailResponse;
import com.aigc.contentfactory.dto.TaskListItemResponse;
import com.aigc.contentfactory.service.TaskFacade;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskFacade taskFacade;

    public TaskController(TaskFacade taskFacade) {
        this.taskFacade = taskFacade;
    }

    @PostMapping
    public ApiResponse<TaskDetailResponse> create(@Valid @RequestBody CreateTaskRequest request) {
        return ApiResponse.success("任务已创建", taskFacade.createTask(request));
    }

    @GetMapping
    public ApiResponse<List<TaskListItemResponse>> list() {
        return ApiResponse.success(taskFacade.listTasks());
    }

    @GetMapping("/{taskId}")
    public ApiResponse<TaskDetailResponse> detail(@PathVariable Long taskId) {
        return ApiResponse.success(taskFacade.getTask(taskId));
    }

    @PostMapping("/{taskId}/approve")
    public ApiResponse<TaskDetailResponse> approve(@PathVariable Long taskId,
                                                   @Valid @RequestBody ReviewActionRequest request) {
        return ApiResponse.success(taskFacade.approveTask(taskId, request.getComment()));
    }

    @PostMapping("/{taskId}/reject")
    public ApiResponse<TaskDetailResponse> reject(@PathVariable Long taskId,
                                                  @Valid @RequestBody ReviewActionRequest request) {
        return ApiResponse.success(taskFacade.rejectTask(taskId, request.getComment()));
    }

    @PostMapping("/{taskId}/publish")
    public ApiResponse<TaskDetailResponse> publish(@PathVariable Long taskId,
                                                   @RequestBody(required = false) PublishRequest request) {
        List<String> platforms = request == null ? null : request.getPlatforms();
        return ApiResponse.success(taskFacade.publishTask(taskId, platforms));
    }
}
