package interfaces;

import model.Epic;
import model.Subtask;
import model.Task;

import java.util.List;
import java.util.Map;

public interface TaskManager {
    // Методы для обычных задач
    Map<Integer, Task> getTasks();
    void deleteAllTasks();
    Task getTaskById(int id);
    void createTask(Task task);
    void updateTask(Task task);
    void deleteTaskById(int id);

    // Методы для эпиков
    Map<Integer, Epic> getEpics();
    void deleteAllEpics();
    Epic getEpicById(int id);
    void createEpic(Epic epic);
    void updateEpic(Epic epic);
    List<Subtask> getSubtasksByEpicId(int id);
    void deleteEpicById(int id);

    // Методы для подзадач
    Map<Integer, Subtask> getSubtasks();
    void deleteAllSubtasks();
    Subtask getSubtaskById(int id);
    void createSubtask(Subtask subtask);
    void updateSubtask(Subtask subtask);
    void deleteSubtaskById(int id);

    // История просмотров
    List<Task> getHistory();

    // Дополнительный метод для получения всех задач
    List<Task> getAllTasks();
}