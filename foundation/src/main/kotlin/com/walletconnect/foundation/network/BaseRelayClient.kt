package com.walletconnect.foundation.network

import com.walletconnect.foundation.common.model.SubscriptionId
import com.walletconnect.foundation.common.model.Topic
import com.walletconnect.foundation.common.model.Ttl
import com.walletconnect.foundation.common.toRelayAcknowledgment
import com.walletconnect.foundation.common.toRelayEvent
import com.walletconnect.foundation.common.toRelayRequest
import com.walletconnect.foundation.di.commonModule
import com.walletconnect.foundation.network.data.service.RelayService
import com.walletconnect.foundation.network.model.Relay
import com.walletconnect.foundation.network.model.RelayDTO
import com.walletconnect.foundation.util.Logger
import com.walletconnect.foundation.util.scope
import com.walletconnect.util.generateId
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import org.koin.core.KoinApplication

abstract class BaseRelayClient : RelayInterface {
    private var foundationKoinApp: KoinApplication = KoinApplication.init()
    lateinit var relayService: RelayService
    private var logger: Logger

    init {
        foundationKoinApp.run { modules(commonModule()) }
        logger = foundationKoinApp.koin.get()
    }

    override val eventsFlow: SharedFlow<Relay.Model.Event> by lazy {
        relayService
            .observeWebSocketEvent()
            .map { event -> event.toRelayEvent() }
            .shareIn(scope, SharingStarted.Lazily, REPLAY)
    }

    override val subscriptionRequest: Flow<Relay.Model.Call.Subscription.Request> by lazy {
        relayService.observeSubscriptionRequest()
            .map { request -> request.toRelayRequest() }
            .onEach { relayRequest -> supervisorScope { publishSubscriptionAcknowledgement(relayRequest.id) } }
    }

    override fun publish(
        topic: String,
        message: String,
        params: Relay.Model.IrnParams,
        onResult: (Result<Relay.Model.Call.Publish.Acknowledgement>) -> Unit,
    ) {
        val (tag, ttl, prompt) = params
        val publishParams = RelayDTO.Publish.Request.Params(Topic(topic), message, Ttl(ttl), tag, prompt)
        val request = RelayDTO.Publish.Request(generateId(), params = publishParams)

        observePublishAcknowledgement { acknowledgement -> onResult(Result.success(acknowledgement)) }
        observePublishError { error -> onResult(Result.failure(error)) }
        relayService.publishRequest(request)
    }

    override fun subscribe(topic: String, onResult: (Result<Relay.Model.Call.Subscribe.Acknowledgement>) -> Unit) {
        val subscribeRequest = RelayDTO.Subscribe.Request(id = generateId(), params = RelayDTO.Subscribe.Request.Params(Topic(topic)))
        observeSubscribeAcknowledgement { acknowledgement -> onResult(Result.success(acknowledgement)) }
        observeSubscribeError { error -> onResult(Result.failure(error)) }
        relayService.subscribeRequest(subscribeRequest)
    }

    override fun unsubscribe(
        topic: String,
        subscriptionId: String,
        onResult: (Result<Relay.Model.Call.Unsubscribe.Acknowledgement>) -> Unit,
    ) {
        val unsubscribeRequest = RelayDTO.Unsubscribe.Request(
            id = generateId(),
            params = RelayDTO.Unsubscribe.Request.Params(Topic(topic), SubscriptionId(subscriptionId))
        )
        observeUnSubscribeAcknowledgement { acknowledgement -> onResult(Result.success(acknowledgement)) }
        observeUnSubscribeError { error -> onResult(Result.failure(error)) }
        relayService.unsubscribeRequest(unsubscribeRequest)
    }

    private fun publishSubscriptionAcknowledgement(id: Long) {
        val publishRequest = RelayDTO.Subscription.Acknowledgement(id = id, result = true)
        relayService.publishSubscriptionAcknowledgement(publishRequest)
    }

    private fun observePublishAcknowledgement(onResult: (Relay.Model.Call.Publish.Acknowledgement) -> Unit) {
        scope.launch {
            relayService.observePublishAcknowledgement()
                .map { ack -> ack.toRelayAcknowledgment() }
                .catch { exception -> logger.error(exception) }
                .collect { acknowledgement ->
                    supervisorScope {
                        onResult(acknowledgement)
                        cancel()
                    }
                }
        }
    }

    private fun observePublishError(onFailure: (Throwable) -> Unit) {
        scope.launch {
            relayService.observePublishError()
                .onEach { jsonRpcError -> logger.error(Throwable(jsonRpcError.error.errorMessage)) }
                .catch { exception -> logger.error(exception) }
                .collect { errorResponse ->
                    supervisorScope {
                        onFailure(Throwable(errorResponse.error.errorMessage))
                        cancel()
                    }
                }
        }
    }

    private fun observeSubscribeAcknowledgement(onResult: (Relay.Model.Call.Subscribe.Acknowledgement) -> Unit) {
        scope.launch {
            relayService.observeSubscribeAcknowledgement()
                .map { ack -> ack.toRelayAcknowledgment() }
                .catch { exception -> logger.error(exception) }
                .collect { acknowledgement ->
                    supervisorScope {
                        onResult(acknowledgement)
                        cancel()
                    }
                }
        }
    }

    private fun observeSubscribeError(onFailure: (Throwable) -> Unit) {
        scope.launch {
            relayService.observeSubscribeError()
                .onEach { jsonRpcError -> logger.error(Throwable(jsonRpcError.error.errorMessage)) }
                .catch { exception -> logger.error(exception) }
                .collect { errorResponse ->
                    supervisorScope {
                        onFailure(Throwable(errorResponse.error.errorMessage))
                        cancel()
                    }
                }
        }
    }

    private fun observeUnSubscribeAcknowledgement(onSuccess: (Relay.Model.Call.Unsubscribe.Acknowledgement) -> Unit) {
        scope.launch {
            relayService.observeUnsubscribeAcknowledgement()
                .map { ack -> ack.toRelayAcknowledgment() }
                .catch { exception -> logger.error(exception) }
                .collect { acknowledgement ->
                    supervisorScope {
                        onSuccess(acknowledgement)
                        cancel()
                    }
                }
        }
    }

    private fun observeUnSubscribeError(onFailure: (Throwable) -> Unit) {
        scope.launch {
            relayService.observeUnsubscribeError()
                .onEach { jsonRpcError -> logger.error(Throwable(jsonRpcError.error.errorMessage)) }
                .catch { exception -> logger.error(exception) }
                .collect { errorResponse ->
                    supervisorScope {
                        onFailure(Throwable(errorResponse.error.errorMessage))
                        cancel()
                    }
                }
        }
    }

    private companion object {
        const val REPLAY: Int = 1
    }
}