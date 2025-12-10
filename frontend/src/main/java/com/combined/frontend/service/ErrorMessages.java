package com.combined.frontend.service;

import java.util.Locale;

/**
 * Maps raw backend error strings to user-friendly messages for the UI.
 */
public final class ErrorMessages {

    private ErrorMessages() {}

    public static String resolve(String raw) {
        if (raw == null || raw.isBlank()) {
            return "Произошла ошибка. Попробуйте ещё раз.";
        }
        String msg = raw.toLowerCase(Locale.ROOT);

        // Auth / profile
        if (msg.contains("email") && msg.contains("exists")) return "Email уже используется.";
        if (msg.contains("nickname") && msg.contains("exists")) return "Никнейм уже используется.";
        if (msg.contains("passwords do not match") || msg.contains("пароли")) return "Пароли не совпадают.";
        if (msg.contains("password must be at least") || msg.contains("password") && msg.contains("short")) return "Пароль должен быть не короче 6 символов.";
        if (msg.contains("invalid credentials") || msg.contains("login failed") || msg.contains("unauthorized")) return "Неверный email или пароль.";

        // Access / security
        if (msg.contains("forbidden") || msg.contains("not allowed")) return "Недостаточно прав для выполнения действия.";
        if (msg.contains("token") && msg.contains("expired")) return "Сессия истекла. Войдите заново.";
        if (msg.contains("token") && msg.contains("invalid")) return "Недействительный токен. Войдите заново.";

        // Groups / membership
        if (msg.contains("group not found")) return "Группа не найдена.";
        if (msg.contains("already a member")) return "Вы уже состоите в этой группе.";
        if (msg.contains("not a member")) return "Требуется членство в группе.";
        if (msg.contains("cannot remove group creator")) return "Нельзя удалить или понизить создателя группы.";
        if (msg.contains("cannot assign creator role")) return "Нельзя назначить роль CREATOR.";

        // Tasks
        if (msg.contains("task not found")) return "Задача не найдена.";
        if (msg.contains("title") && msg.contains("blank")) return "Название задачи не может быть пустым.";
        if (msg.contains("status") && msg.contains("blank")) return "Статус задачи обязателен.";
        if (msg.contains("not a member of this group")) return "Нет доступа к задачам этой группы.";

        // Resources
        if (msg.contains("resource not found")) return "Материал не найден.";
        if (msg.contains("only admins/creator can add resources")) return "Добавлять материалы могут только админ или создатель группы.";
        if (msg.contains("only group members can view resources")) return "Материалы доступны только участникам группы.";

        // Activity / stats
        if (msg.contains("activity") && msg.contains("member")) return "Логи доступны только участникам группы.";
        if (msg.contains("stats") && msg.contains("member")) return "Статистика доступна только участникам группы.";

        // Generic fallback with original
        return raw;
    }
}
