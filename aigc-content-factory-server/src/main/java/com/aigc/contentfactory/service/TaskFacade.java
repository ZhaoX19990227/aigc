package com.aigc.contentfactory.service;

import com.aigc.contentfactory.dto.CreateTaskRequest;
import com.aigc.contentfactory.dto.TaskDetailResponse;
import com.aigc.contentfactory.dto.TaskListItemResponse;
import java.util.List;

public interface TaskFacade {

    TaskDetailResponse createTask(CreateTaskRequest request);

    List<TaskListItemResponse> listTasks();

    TaskDetailResponse getTask(Long taskId);

    TaskDetailResponse approveTask(Long taskId, String comment);

    TaskDetailResponse rejectTask(Long taskId, String comment);

    TaskDetailResponse publishTask(Long taskId, List<String> platforms);
}
