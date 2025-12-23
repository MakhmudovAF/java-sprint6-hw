package manager;

import enums.Status;
import enums.TaskType;
import model.Epic;
import model.Subtask;
import model.Task;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class FileBackedTasksManager extends InMemoryTaskManager {
    private final File file;

    public FileBackedTasksManager(File file) {
        this.file = file;
    }

    protected void save() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8))) {
            writer.write("id,type,name,status,description,epic");
            writer.newLine();

            saveTasks(writer);

            writer.newLine();

            saveHistory(writer);

        } catch (IOException e) {
            throw new ManagerSaveException("Ошибка при сохранении в файл", e);
        }
    }

    private void saveTasks(BufferedWriter writer) throws IOException {
        for (Task task : super.getTasks().values()) {
            writer.write(taskToString(task));
            writer.newLine();
        }

        for (Epic epic : super.getEpics().values()) {
            writer.write(taskToString(epic));
            writer.newLine();
        }

        for (Subtask subtask : super.getSubtasks().values()) {
            writer.write(taskToString(subtask));
            writer.newLine();
        }
    }

    private void saveHistory(BufferedWriter writer) throws IOException {
        List<Task> history = super.getHistory();
        if (!history.isEmpty()) {
            List<Integer> historyIds = new ArrayList<>();
            for (Task task : history) {
                historyIds.add(task.getId());
            }
            writer.write(historyToString(historyIds));
        }
    }

    private String taskToString(Task task) {
        if (task instanceof Epic) {
            return String.format("%d,%s,%s,%s,%s,",
                    task.getId(),
                    TaskType.EPIC,
                    task.getName(),
                    task.getStatus(),
                    task.getDescription());
        } else if (task instanceof Subtask) {
            Subtask subtask = (Subtask) task;
            return String.format("%d,%s,%s,%s,%s,%d",
                    task.getId(),
                    TaskType.SUBTASK,
                    task.getName(),
                    task.getStatus(),
                    task.getDescription(),
                    subtask.getEpicId());
        } else {
            return String.format("%d,%s,%s,%s,%s,",
                    task.getId(),
                    TaskType.TASK,
                    task.getName(),
                    task.getStatus(),
                    task.getDescription());
        }
    }

    private static Task taskFromString(String value) {
        String[] parts = value.split(",");
        int id = Integer.parseInt(parts[0]);
        TaskType type = TaskType.valueOf(parts[1]);
        String name = parts[2];
        Status status = Status.valueOf(parts[3]);
        String description = parts[4];

        switch (type) {
            case TASK:
                Task task = new Task(name, description, status);
                task.setId(id);
                return task;

            case EPIC:
                Epic epic = new Epic(name, description);
                epic.setId(id);
                epic.setStatus(status);
                return epic;

            case SUBTASK:
                int epicId = Integer.parseInt(parts[5]);
                Subtask subtask = new Subtask(name, description, status, epicId);
                subtask.setId(id);
                return subtask;

            default:
                throw new IllegalArgumentException("Неизвестный тип задачи: " + type);
        }
    }

    private static String historyToString(List<Integer> historyIds) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < historyIds.size(); i++) {
            sb.append(historyIds.get(i));
            if (i < historyIds.size() - 1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    private static List<Integer> historyFromString(String value) {
        List<Integer> historyIds = new ArrayList<>();
        if (value == null || value.isBlank()) {
            return historyIds;
        }

        String[] parts = value.split(",");
        for (String part : parts) {
            historyIds.add(Integer.parseInt(part.trim()));
        }
        return historyIds;
    }

    public static FileBackedTasksManager loadFromFile(File file) {
        FileBackedTasksManager manager = new FileBackedTasksManager(file);

        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String line;
            boolean readingHistory = false;
            List<Integer> historyIds = new ArrayList<>();

            // Пропускаем заголовок
            reader.readLine();

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty()) {
                    readingHistory = true;
                    continue;
                }

                if (!readingHistory) {
                    // Читаем задачу
                    Task task = taskFromString(line);
                    addTaskToManager(manager, task);
                } else {
                    // Читаем историю
                    historyIds = historyFromString(line);
                    break;
                }
            }

            // Восстанавливаем связи эпиков и подзадач
            restoreEpicSubtaskRelations(manager);

            // Восстанавливаем историю
            restoreHistory(manager, historyIds);

            // Обновляем nextId
            updateNextId(manager);

        } catch (IOException e) {
            throw new ManagerSaveException("Ошибка при загрузке из файла", e);
        }

        return manager;
    }

    private static void addTaskToManager(FileBackedTasksManager manager, Task task) {
        if (task instanceof Epic) {
            manager.epics.put(task.getId(), (Epic) task);
        } else if (task instanceof Subtask) {
            manager.subtasks.put(task.getId(), (Subtask) task);
        } else {
            manager.tasks.put(task.getId(), task);
        }
    }

    private static void restoreHistory(FileBackedTasksManager manager, List<Integer> historyIds) {
        for (int id : historyIds) {
            Task task = manager.findTaskById(id);
            if (task != null) {
                manager.history.add(task);
            }
        }
    }

    private static void restoreEpicSubtaskRelations(FileBackedTasksManager manager) {
        for (Subtask subtask : manager.subtasks.values()) {
            Epic epic = manager.epics.get(subtask.getEpicId());
            if (epic != null) {
                epic.addSubtaskId(subtask.getId());
            }
        }
    }

    private static void updateNextId(FileBackedTasksManager manager) {
        int maxId = 0;

        for (Task task : manager.getAllTasks()) {
            if (task.getId() > maxId) {
                maxId = task.getId();
            }
        }

        manager.nextId = maxId + 1;
    }

    private Task findTaskById(int id) {
        Task task = tasks.get(id);
        if (task != null) return task;

        task = epics.get(id);
        if (task != null) return task;

        return subtasks.get(id);
    }

    // Переопределяем все методы, изменяющие состояние

    @Override
    public void createTask(Task task) {
        super.createTask(task);
        save();
    }

    @Override
    public void updateTask(Task task) {
        super.updateTask(task);
        save();
    }

    @Override
    public void deleteTaskById(int id) {
        super.deleteTaskById(id);
        save();
    }

    @Override
    public void deleteAllTasks() {
        super.deleteAllTasks();
        save();
    }

    @Override
    public void createEpic(Epic epic) {
        super.createEpic(epic);
        save();
    }

    @Override
    public void updateEpic(Epic epic) {
        super.updateEpic(epic);
        save();
    }

    @Override
    public void deleteEpicById(int id) {
        super.deleteEpicById(id);
        save();
    }

    @Override
    public void deleteAllEpics() {
        super.deleteAllEpics();
        save();
    }

    @Override
    public void createSubtask(Subtask subtask) {
        super.createSubtask(subtask);
        save();
    }

    @Override
    public void updateSubtask(Subtask subtask) {
        super.updateSubtask(subtask);
        save();
    }

    @Override
    public void deleteSubtaskById(int id) {
        super.deleteSubtaskById(id);
        save();
    }

    @Override
    public void deleteAllSubtasks() {
        super.deleteAllSubtasks();
        save();
    }

    @Override
    public Task getTaskById(int id) {
        Task task = super.getTaskById(id);
        save();
        return task;
    }

    @Override
    public Epic getEpicById(int id) {
        Epic epic = super.getEpicById(id);
        save();
        return epic;
    }

    @Override
    public Subtask getSubtaskById(int id) {
        Subtask subtask = super.getSubtaskById(id);
        save();
        return subtask;
    }

    public static void main(String[] args) {
        System.out.println("=== Тестирование FileBackedTasksManager ===\n");

        // Используем временный файл
        File file = new File("test_tasks.csv");

        try {
            System.out.println("1. Создаем менеджер и добавляем задачи:");
            FileBackedTasksManager manager1 = new FileBackedTasksManager(file);

            // Создаем задачи
            Task task1 = new Task("Задача 1", "Описание задачи 1", Status.NEW);
            manager1.createTask(task1);

            Epic epic1 = new Epic("Эпик 1", "Описание эпика 1");
            manager1.createEpic(epic1);

            Subtask subtask1 = new Subtask("Подзадача 1", "Описание подзадачи 1", Status.NEW, epic1.getId());
            Subtask subtask2 = new Subtask("Подзадача 2", "Описание подзадачи 2", Status.IN_PROGRESS, epic1.getId());
            manager1.createSubtask(subtask1);
            manager1.createSubtask(subtask2);

            Task task2 = new Task("Задача 2", "Описание задачи 2", Status.DONE);
            manager1.createTask(task2);

            System.out.println("Созданы задачи:");
            System.out.println("- Обычные задачи: " + manager1.getTasks().size());
            System.out.println("- Эпики: " + manager1.getEpics().size());
            System.out.println("- Подзадачи: " + manager1.getSubtasks().size());

            System.out.println("\n2. Заполняем историю просмотров:");
            manager1.getTaskById(task1.getId());
            manager1.getEpicById(epic1.getId());
            manager1.getSubtaskById(subtask1.getId());
            manager1.getTaskById(task2.getId());
            manager1.getTaskById(task1.getId());

            System.out.println("История просмотров:");
            for (Task task : manager1.getHistory()) {
                System.out.println("- " + task.getName() + " (ID: " + task.getId() + ")");
            }

            System.out.println("\n3. Состояние сохранено в файл: " + file.getAbsolutePath());
            System.out.println("Файл существует: " + file.exists());
            System.out.println("Размер файла: " + file.length() + " байт");

            // Показываем содержимое файла
            System.out.println("\nСодержимое файла:");
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }

            System.out.println("\n4. Загружаем данные из файла в новый менеджер:");
            FileBackedTasksManager manager2 = FileBackedTasksManager.loadFromFile(file);

            System.out.println("\n5. Проверяем восстановление данных:");
            System.out.println("- Обычные задачи восстановлены: " + manager2.getTasks().size());
            System.out.println("- Эпики восстановлены: " + manager2.getEpics().size());
            System.out.println("- Подзадачи восстановлены: " + manager2.getSubtasks().size());

            // Проверяем, что все задачи совпадают
            System.out.println("\n6. Проверяем содержимое задач:");
            for (Task task : manager2.getTasks().values()) {
                System.out.println("Задача: " + task.getName() + ", статус: " + task.getStatus());
            }

            for (Epic epic : manager2.getEpics().values()) {
                System.out.println("Эпик: " + epic.getName() + ", статус: " + epic.getStatus() +
                        ", подзадач: " + epic.getSubtaskIds().size());
            }

            for (Subtask subtask : manager2.getSubtasks().values()) {
                System.out.println("Подзадача: " + subtask.getName() + ", статус: " + subtask.getStatus() +
                        ", эпик: " + subtask.getEpicId());
            }

            System.out.println("\n7. Проверяем восстановление истории:");
            System.out.println("История восстановлена (количество записей): " + manager2.getHistory().size());
            System.out.println("История просмотров:");
            for (Task task : manager2.getHistory()) {
                System.out.println("- " + task.getName() + " (ID: " + task.getId() + ")");
            }

            // Проверяем, что можем продолжать работу с восстановленным менеджером
            System.out.println("\n8. Тестируем работу восстановленного менеджера:");
            Task newTask = new Task("Новая задача", "Добавлена после загрузки", Status.NEW);
            manager2.createTask(newTask);
            System.out.println("Добавлена новая задача с ID: " + newTask.getId());

        } catch (Exception e) {
            System.out.println("Ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }
}