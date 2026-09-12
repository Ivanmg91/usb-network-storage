package com.usblocal.app.di

import com.usblocal.app.data.smb.SmbDataSource
import com.usblocal.app.data.smb.SmbDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindSmbDataSource(impl: SmbDataSourceImpl): SmbDataSource
}
