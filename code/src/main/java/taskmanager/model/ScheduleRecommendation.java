package taskmanager.model;

/**
 * Represents a scheduling recommendation for a specific task.
 * <p>
 * This record couples the original {@link Task} with a recommendation
 * message that explains whether the task is safe to execute as planned
 * or should be rescheduled due to weather conditions.
 * </p>
 *
 * @param task the task related to the recommendation
 * @param recommendation the recommendation message
 */
public record ScheduleRecommendation(Task task, String recommendation) {
}