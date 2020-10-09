package com.microsoft.did.sdk.credential.models.receipts

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ReceiptAction {
    Issuance,
    Presentation,
    Revocation
}

@Entity
data class Receipt(

    val action: ReceiptAction,

    // did of the verifier/issuer
    val entityIdentifier: String,

    //Name of the verifier/issuer
    val entityName: String,

    val vcId: String,

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // date action occurred
    val activityDate: Long = System.currentTimeMillis()
)