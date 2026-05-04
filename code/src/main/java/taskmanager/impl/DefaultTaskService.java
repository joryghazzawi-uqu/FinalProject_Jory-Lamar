package taskmanager.impl;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import taskmanager.api.StorageService;
import taskmanager.api.TaskService;
import taskmanager.exception.InvalidTaskException;
import taskmanager.exception.TaskNotFoundException;
import taskmanager.model.Task;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of {@link TaskService}.
 * <p>
 * Stores tasks in a thread-safe {@link ConcurrentHashMap} and persists them
 * through a {@link StorageService}. Reactor ensures file operations execute
 * off the Swing Event Dispatch Thread.
 * </p>
 */
public class DefaultTaskService implements TaskService {
    /** In-memory task store keyed by task ID. */
    private final Map<String, Task> tasks = new ConcurrentHashMap<>();

    /** Service used to persist and load tasks. */
    private final StorageService storageService;

    /**
     * Creates the service and loads existing tasks from storage.
     *
     * @param storageService storage service used to load and save tasks; must not be {@code null}
     */
    public DefaultTaskService(StorageService storageService) {
        this.storageService = storageService;

        storageService.loadTasks()
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(loadedTasks ->
                        loadedTasks.forEach(task -> tasks.put(task.getId(), task))
                )
                .doOnError(error ->
                        System.err.println("Failed to load tasks: " + error.getMessage())
                )
                .subscribe();
    }

    /**
     * Adds or updates a task asynchronously.
     * <p>
     * Preconditions: {@code task} must not be {@code null} and must satisfy
     * <code>validateTask</code> rules.
     * </p>
     * <p>
     * Postconditions: the task is stored in memory and persisted to disk.
     * </p>
     * <p>
     * Side effects: may update the in-memory task map and invoke the storage service.
     * </p>
     *
     * @param task task to add or update
     * @return Mono that completes after validation, memory update, and file save
     * @throws InvalidTaskException if task data is invalid
     */
    @Override
    public Mono<Void> addTask(Task task) {
        return Mono.fromRunnable(() -> validateTask(task))
                .then(Mono.fromRunnable(() -> tasks.put(task.getId(), task)))
                .then(Mono.defer(() -> storageService.saveTasks(List.copyOf(tasks.values()))))
                .then()
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Removes a task asynchronously.
     * <p>
     * Preconditions: {@code taskId} must not be {@code null} or blank.
     * </p>
     * <p>
     * Postconditions: the task is removed from memory and persisted storage
     * if it exists.
     * </p>
     * <p>
     * Side effects: may modify the in-memory task map and save the remaining tasks.
     * </p>
     *
     * @param taskId task ID
     * @return Mono that completes after removal and file save
     * @throws InvalidTaskException if taskId is empty
     * @throws TaskNotFoundException if taskId does not exist
     */
    @Override
    public Mono<Void> removeTask(String taskId) {
        return Mono.fromRunnable(() -> {
                    if (taskId == null || taskId.isBlank()) {
                        throw new InvalidTaskException("Task ID must not be empty");
                    }

                    if (tasks.remove(taskId) == null) {
                        throw new TaskNotFoundException(taskId);
                    }
                })
                .then(Mono.defer(() -> storageService.saveTasks(List.copyOf(tasks.values()))))
                .then()
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Finds a task by ID.
     * <p>
     * Preconditions: {@code taskId} must not be {@code null} or blank.
     * </p>
     * <p>
     * Postconditions: if a task exists, it is emitted by the returned Mono.
     * </p>
     *
     * @param taskId task ID
     * @return Mono emitting the task
     * @throws TaskNotFoundException if no task exists with the given ID
     */
    @Override
    public Mono<Task> findTaskById(String taskId) {
        return Mono.justOrEmpty(tasks.get(taskId))
                .switchIfEmpty(Mono.error(new TaskNotFoundException(taskId)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Returns all tasks as a Flux.
     * <p>
     * Postconditions: the returned Flux emits a snapshot of the current tasks.
     * </p>
     *
     * @return Flux containing all tasks
     */
    @Override
    public Flux<Task> findAllTasks() {
        return Flux.defer(() -> Flux.fromIterable(new ArrayList<>(tasks.values())))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Returns all tasks as a List inside a Mono.
     * <p>
     * Postconditions: the returned Mono emits a list snapshot of all current tasks.
     * </p>
     *
     * @return Mono emitting list of tasks
     */
    @Override
    public Mono<List<Task>> findAllTasksAsList() {
        return Mono.<List<Task>>fromCallable(() -> new ArrayList<>(tasks.values()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Validates task data before saving.
     * <p>
     * Preconditions: the {@code task} parameter may be {@code null}.
     * </p>
     * <p>
     * Postconditions: if no exception is thrown, the task meets the validation
     * requirements for persistence.
     * </p>
     *
     * @param task task to validate
     * @throws InvalidTaskException if validation fails
     */
    private void validateTask(Task task) {
        if (task == null) {
            throw new InvalidTaskException("Task cannot be null");
        }

        if (task.getId() == null || task.getId().isBlank()) {
            throw new InvalidTaskException("Task ID must not be empty");
        }

        if (task.getTitle() == null || task.getTitle().isBlank()) {
            throw new InvalidTaskException("Task title must not be empty");
        }

        if (task.getDueDateTime() == null) {
            throw new InvalidTaskException("Task due date and time must be set");
        }

        if (task.getDueDateTime().isBefore(LocalDateTime.now())) {
            throw new InvalidTaskException("Task due date must be in the future");
        }
    }
}
