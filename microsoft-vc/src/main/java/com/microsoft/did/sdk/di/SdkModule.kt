/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package com.microsoft.did.sdk.di

import android.content.Context
import androidx.room.Room
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.microsoft.did.sdk.credential.service.validators.OidcPresentationRequestValidator
import com.microsoft.did.sdk.credential.service.validators.PresentationRequestValidator
import com.microsoft.did.sdk.crypto.CryptoOperations
import com.microsoft.did.sdk.crypto.keyStore.AndroidKeyStore
import com.microsoft.did.sdk.crypto.keyStore.KeyStore
import com.microsoft.did.sdk.crypto.keys.ellipticCurve.EllipticCurvePairwiseKey
import com.microsoft.did.sdk.crypto.models.webCryptoApi.SubtleCrypto
import com.microsoft.did.sdk.crypto.models.webCryptoApi.W3cCryptoApiConstants
import com.microsoft.did.sdk.crypto.plugins.AndroidSubtle
import com.microsoft.did.sdk.crypto.plugins.EllipticCurveSubtleCrypto
import com.microsoft.did.sdk.crypto.plugins.SubtleCryptoMapItem
import com.microsoft.did.sdk.crypto.plugins.SubtleCryptoScope
import com.microsoft.did.sdk.datasource.db.DbMigrations
import com.microsoft.did.sdk.datasource.db.SdkDatabase
import com.microsoft.did.sdk.identifier.registrars.Registrar
import com.microsoft.did.sdk.identifier.registrars.SidetreeRegistrar
import com.microsoft.did.sdk.util.log.SdkLog
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import javax.inject.Singleton

/**
 * This Module is used by Dagger to provide additional types to the internal dependency graph. Usually types can be
 * provided just by annotating a class constructor with @Inject. However, this is not always possible. It's not when
 * a) Classes need special initialization outside of their constructor
 * b) Multiple instances of the same type are needed (annotate with @Named)
 * c) Interface types don't have a constructor, as such a @Provides annotation has to tell Dagger how to initialize
 *    a type that implements this interface.
 *
 * More information:
 * https://dagger.dev/users-guide
 * https://developer.android.com/training/dependency-injection
 */
@Module
internal class SdkModule {

    @Provides
    @Singleton
    fun defaultCryptoOperations(
        subtleCrypto: SubtleCrypto,
        ecSubtle: EllipticCurveSubtleCrypto,
        keyStore: KeyStore,
        ecPairwiseKey: EllipticCurvePairwiseKey
    ): CryptoOperations {
        val defaultCryptoOperations = CryptoOperations(subtleCrypto, keyStore, ecPairwiseKey)
        defaultCryptoOperations.subtleCryptoFactory.addMessageSigner(
            name = W3cCryptoApiConstants.EcDsa.value,
            subtleCrypto = SubtleCryptoMapItem(ecSubtle, SubtleCryptoScope.ALL)
        )
        defaultCryptoOperations.subtleCryptoFactory.addMessageAuthenticationCodeSigner(
            name = W3cCryptoApiConstants.Hmac.value,
            subtleCrypto = SubtleCryptoMapItem(subtleCrypto, SubtleCryptoScope.ALL)
        )
        return defaultCryptoOperations
    }

    @Provides
    @Singleton
    fun defaultOkHttpClient(): OkHttpClient {
        val httpLoggingInterceptor = HttpLoggingInterceptor { SdkLog.d(it) }
        return OkHttpClient()
            .newBuilder()
            .addInterceptor(httpLoggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun defaultRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("http://TODO.me")
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .build()
    }

    @Provides
    @Singleton
    fun defaultRegistrar(registrar: SidetreeRegistrar): Registrar {
        return registrar
    }

    @Provides
    @Singleton
    fun defaultSubtleCrypto(subtle: AndroidSubtle): SubtleCrypto {
        return subtle
    }

    @Provides
    @Singleton
    fun defaultKeyStore(keyStore: AndroidKeyStore): KeyStore {
        return keyStore
    }

    @Provides
    @Singleton
    fun sdkDatabase(context: Context): SdkDatabase {
        return Room.databaseBuilder(context, SdkDatabase::class.java, "VerifiableCredential-db")
            .addMigrations(DbMigrations.MIGRATION_2_3)
            .fallbackToDestructiveMigration() // TODO: we don't want this here as soon as we go into production
            .build()
    }

    @Provides
    @Singleton
    fun defaultValidator(validator: OidcPresentationRequestValidator): PresentationRequestValidator {
        return validator
    }
}