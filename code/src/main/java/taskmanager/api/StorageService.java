package taskmanager.api;

import reactor.core.publisher.Mono;
import taskmanager.model.Task;

import java.util.List;

/**
 * Handles task persistence across the application.
 * <p>
 * This interface abstracts the underlying storage mechanism used to save
 * and load task data, allowing the task service to remain storage-agnostic.
 * </p>
 */
public interface StorageService {

    /**
     * Saves tasks to persistent storage.
     * <p>
     * Preconditions: {@code tasks} must not be {@code null}.
     * </p>
     * <p>
     * Postconditions: the provided task list is persisted.
     * </p>
     *
     * @param tasks the tasks to save
     * @return a Mono that completes when saving finishes
     */
    Mono<Void> saveTasks(List<Task> tasks);

    /**
     * Loads tasks from persistent storage.
     * <p>
     * Postconditions: the returned {@link Mono} emits the current persisted tasks
     * as a list.
     * </p>
     *
     * @return a Mono emitting the loaded tasks
     */
    Mono<List<Task>> loadTasks();
}
