package kz.abdiyev.walletai.data.mapper

import kz.abdiyev.walletai.data.database.CategoryEntity
import kz.abdiyev.walletai.data.database.TransactionEntity
import kz.abdiyev.walletai.domain.model.CategoryData
import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.domain.model.Money
import kz.abdiyev.walletai.domain.model.TransactionData
import kz.abdiyev.walletai.domain.model.TransactionType

fun CategoryEntity.toDomain(): CategoryData = CategoryData(
    id = id,
    name = name,
    type = TransactionType.fromDb(type),
    colorHex = colorHex,
    iconName = iconName,
    isSystem = isSystem,
    parentId = parentId
)

fun CategoryData.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    type = type.dbValue,
    colorHex = colorHex,
    iconName = iconName,
    isSystem = isSystem,
    parentId = parentId
)

fun TransactionEntity.toDomain(): TransactionData = TransactionData(
    id = id,
    money = Money(amountMinor, Currency.fromCode(currencyCode)),
    type = TransactionType.fromDb(type),
    categoryId = categoryId,
    description = description,
    date = date,
    isAutoCategorized = isAutoCategorized
)

fun TransactionData.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    amountMinor = money.amountMinor,
    currencyCode = money.currency.code,
    type = type.dbValue,
    categoryId = categoryId,
    description = description,
    date = date,
    isAutoCategorized = isAutoCategorized
)
