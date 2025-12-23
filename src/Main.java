import interfaces.TaskManager;
import manager.Managers;
import model.Epic;
import model.Subtask;
import model.Task;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        TaskManager manager = Managers.getDefault();

        System.out.println("=== Тестирование истории просмотров с CustomLinkedList ===\n");

        // Создаем задачи
        System.out.println("1. Создание задач:");

        Task task1 = new Task("Задача 1", "Описание задачи 1", "NEW");
        Task task2 = new Task("Задача 2", "Описание задачи 2", "NEW");
        manager.createTask(task1);
        manager.createTask(task2);

        Epic epic1 = new Epic("Эпик 1", "Описание эпика 1");
        manager.createEpic(epic1);

        Subtask subtask1 = new Subtask("Подзадача 1", "Описание подзадачи 1", "NEW", epic1.getId());
        manager.createSubtask(subtask1);

        System.out.println("Созданы задачи с ID: " + task1.getId() + ", " + task2.getId() +
                ", эпик: " + epic1.getId() + ", подзадача: " + subtask1.getId());

        // Тестируем историю просмотров
        System.out.println("\n2. Добавление задач в историю:");

        System.out.println("Просмотр task1 (ID=" + task1.getId() + ")");
        manager.getTaskById(task1.getId());
        printHistory(manager.getHistory());

        System.out.println("Просмотр task2 (ID=" + task2.getId() + ")");
        manager.getTaskById(task2.getId());
        printHistory(manager.getHistory());

        System.out.println("Просмотр epic1 (ID=" + epic1.getId() + ")");
        manager.getEpicById(epic1.getId());
        printHistory(manager.getHistory());

        System.out.println("Просмотр subtask1 (ID=" + subtask1.getId() + ")");
        manager.getSubtaskById(subtask1.getId());
        printHistory(manager.getHistory());

        // Тестируем удаление дублей
        System.out.println("\n3. Тест удаления дублей (O(1)):");

        System.out.println("Снова просматриваем task1 (должен переместиться в конец)");
        manager.getTaskById(task1.getId());
        printHistory(manager.getHistory());

        System.out.println("Снова просматриваем epic1 (должен переместиться в конец)");
        manager.getEpicById(epic1.getId());
        printHistory(manager.getHistory());

        // Тестируем удаление задач из истории
        System.out.println("\n4. Тест удаления задач:");

        System.out.println("Удаляем task2 (ID=" + task2.getId() + ")");
        manager.deleteTaskById(task2.getId());
        printHistory(manager.getHistory());

        System.out.println("Создаем еще одну задачу и просматриваем её");
        Task task3 = new Task("Задача 3", "Описание задачи 3", "NEW");
        manager.createTask(task3);
        manager.getTaskById(task3.getId());
        printHistory(manager.getHistory());

        // Тестируем большое количество просмотров
        System.out.println("\n5. Тест большого количества просмотров:");

        // Делаем много просмотров одной задачи
        for (int i = 0; i < 20; i++) {
            manager.getTaskById(task1.getId());
        }

        System.out.println("После 20 просмотров task1 история должна содержать только последний просмотр:");
        printHistory(manager.getHistory());

        // Добавляем несколько разных просмотров
        for (int i = 0; i < 5; i++) {
            manager.getEpicById(epic1.getId());
            manager.getSubtaskById(subtask1.getId());
            manager.getTaskById(task3.getId());
        }

        System.out.println("\nПосле чередующихся просмотров:");
        printHistory(manager.getHistory());

        // Тестируем удаление эпика (должны удалиться и его подзадачи из истории)
        System.out.println("\n6. Тест каскадного удаления:");

        System.out.println("Текущая история:");
        printHistory(manager.getHistory());

        System.out.println("Удаляем эпик (ID=" + epic1.getId() + ") - должен удалиться и subtask1 из истории");
        manager.deleteEpicById(epic1.getId());
        printHistory(manager.getHistory());

        System.out.println("\n7. Финальная история:");
        printHistory(manager.getHistory());

        System.out.println("\nВсего задач в истории: " + manager.getHistory().size());

        // Демонстрация работы CustomLinkedList
        System.out.println("\n=== Проверка порядка элементов в истории ===");
        System.out.println("Порядок должен соответствовать порядку последних просмотров:");
        List<Task> history = manager.getHistory();
        for (int i = 0; i < history.size(); i++) {
            System.out.println((i + 1) + ". " + history.get(i).getName() + " (ID: " + history.get(i).getId() + ")");
        }
    }

    private static void printHistory(List<Task> history) {
        if (history.isEmpty()) {
            System.out.println("  История пуста");
            return;
        }

        System.out.println("  История (" + history.size() + "):");
        for (Task task : history) {
            System.out.println("    - " + task.getName() + " (ID: " + task.getId() + ")");
        }
    }
}