package kz.abdiyev.walletai.statement

/** Real text extracted from bank PDFs (see bank_statement/ in repo root). */
object StatementFixtures {
    const val KASPI_RAW = """
АО «Kaspi Bank», БИК CASPKZKA, www.kaspi.kz
Приложение к Справке №1234324198 от 07 июля 2026
30.06.26 + 470,00 ₸ Пополнение Сұлтан С.
30.06.26 - 2 000,00 ₸ Снятие Банкомат Otdelenie Kaspi.kz
30.06.26 + 265,00 ₸ Пополнение С карты другого банка
29.06.26 - 15,00 ₸ Покупка Живая Вода
26.06.26 - 1 020,00 ₸ Перевод на свой Оплата Kaspi Red
счет
26.06.26 + 100,00 ₸ Пополнение С карты другого банка
26.06.26 - 1,00 ₸ Покупка Beeline
23.06.26 - 149,00 ₸ Разное Комиссия за перевод на карту др. банка
10.06.26 + 550 000,00 ₸ Пополнение Зарплата
10.06.26 - 28 875,00 ₸ Перевод на свой Оплата Kaspi Red
счет
07.06.26 - 16 074,75 ₸ Покупка Glovo Kazakhstan
-- 1 of 2 --
"""

    const val FREEDOM_RAW = """
Подлинность справки можете проверить
https://bankffin.kz/ru/check-receipt
Дата Сумма Валюта Операция Детали
28.06.2026 -4,000.00 ₸ KZT Сумма в
обработке Перевод с карты на карту
28.06.2026 -110.00 ₸ KZT Сумма в
обработке FRPAY WHOOSH ALMATY KZ
28.06.2026 -240.00 ₸ KZT Платеж
Payment for ОНАЙ - Транспортная
карта г.Алматы with requisites
account=9643108503328421815 and
total amount of 240.0KZT
28.06.2026 -2,262.00 ₸ KZT Сумма в
обработке MAGNUM.AF50.KASSA-5 ALMATY KZ
27.06.2026 -15,500.00 ₸ KZT Сумма в
обработке KFC 12776 ALMATY KZ
27.06.2026 +19,840.00 ₸ KZT Пополнение Перевод с карты на карту
26.06.2026 -2,500.00 ₸ KZT Покупка SELI POYELI CANTEEN ALMATY KZ
22.06.2026 +2.26 ${'$'} USD Перевод
Перевод валюты Freedom на счет
KZ96551H600001477635.
-- 1 of 3 --
"""
}
