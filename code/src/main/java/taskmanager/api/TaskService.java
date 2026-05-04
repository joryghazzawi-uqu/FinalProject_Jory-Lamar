package taskmanager.api;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import taskmanager.model.Task;

import java.util.List;

/**
 * Service interface responsible for task CRUD operations.
 * <p>
 * This service exposes reactive operations for saving, removing, and
 * retrieving tasks, while isolating storage-specific details from callers.
 * </p>
 */
public interface TaskService {

    /**
     * Adds or updates a task asynchronously.
     * <p>
     * Preconditions: {@code task} must not be {@code null} and must contain a
     * valid task identifier.
     * </p>
     * <p>
     * Postconditions: the task is stored in persistent storage and will be
     * available through subsequent retrieval operations.
     * </p>
     * <p>
     * Side effects: performs validation and persists data.
     * </p>
     *
     * @param task the task to add or update
     * @return a Mono that completes when the task is saved
     * @throws taskmanager.exception.InvalidTaskException if the task data is invalid
     */
    Mono<Void> addTask(Task task);

    /**
     * Removes a task asynchronously by ID.
     * <p>
     * Preconditions: {@code taskId} must not be {@code null} or blank.
     * </p>
     * <p>
     * Postconditions: the task is removed from storage if it exists.
     * </p>
     * <p>
     * Side effects: may modify persistent task storage.
     * </p>
     *
     * @param taskId the task ID
     * @return a Mono that completes when the task is removed
     * @throws taskmanager.exception.InvalidTaskException if the task ID is invalid
     * @throws taskmanager.exception.TaskNotFoundException if the task does not exist
     */
    Mono<Void> removeTask(String taskId);

    /**
     * Finds a task by its ID.
     * <p>
     * Preconditions: {@code taskId} must not be {@code null} or blank.
     * </p>
     * <p>
     * Postconditions: if a task with the given ID exists, it is emitted by
     * the returned {@link Mono}.
     * </p>
     *
     * @param taskId the task ID
     * @return a Mono emitting the task if found
     * @throws taskmanager.exception.TaskNotFoundException if the task does not exist
     */
    Mono<Task> findTaskById(String taskId);

    /**
     * Returns all tasks as a reactive stream.
     * <p>
     * Preconditions: the task collection must be accessible to the storage layer.
     * </p>
     *
     * @return a Flux emitting all stored tasks
     */
    Flux<Task> findAllTasks();

    /**
     * Returns all tasks as a List wrapped in a Mono.
     * <p>
     * Postconditions: the returned Mono emits a snapshot list of current tasks.
     * </p>
     *
     * @return a Mono emitting a list of all tasks
     */
    Mono<List<Task>> findAllTasksAsList();
}
