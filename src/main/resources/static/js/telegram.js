window.addEventListener("DOMContentLoaded", async () => {
    const tg = window.Telegram.WebApp;
    const initData = tg.initData;

    if (!initData || initData.length === 0) {
        document.getElementById("user-info").innerHTML = "<p class='text-red-500'>Ошибка: нет initData</p>";
        return;
    }

    try {
        const res = await fetch("/auth/telegram", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(Object.fromEntries(new URLSearchParams(initData)))
        });

        if (!res.ok) throw new Error("Ошибка запроса");

        const user = await res.json();

        document.getElementById("user-info").innerHTML = `
      <h1 class="text-xl font-bold">Привет, ${user.firstName}!</h1>
      <p>ID: ${user.id}</p>
      <p>Username: @${user.username || "не указан"}</p>
    `;
    } catch (err) {
        document.getElementById("user-info").innerHTML = `<p class="text-red-500">Ошибка авторизации</p>`;
    }
});
