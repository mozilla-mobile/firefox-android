/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components

import android.content.Context
import mozilla.appservices.fxaclient.FxaServer
import mozilla.components.service.fxa.ServerConfig
import org.mozilla.fenix.Config
import org.mozilla.fenix.ext.settings

/**
 * Utility to configure Firefox Account servers.
 */

object FxaServer {
    private const val CLIENT_ID = "a2270f727f45f648"
    const val REDIRECT_URL = "urn:ietf:wg:oauth:2.0:oob:oauth-redirect-webchannel"

    fun config(context: Context): ServerConfig {
        // If a server override is configured, use that. Otherwise:
        // - for all channels other than Mozilla Online, use FxaServer.Release.
        // - for Mozilla Online channel, if domestic server is allowed, use FxaServer.China; otherwise,
        //   use FxaServer.Release
        val serverOverride = context.settings().overrideFxAServer
        val tokenServerOverride = context.settings().overrideSyncTokenServer.ifEmpty { null }
        if (serverOverride.isEmpty()) {
            val releaseServer = if (Config.channel.isMozillaOnline && context.settings().allowDomesticChinaFxaServer) {
                FxaServer.China
            } else {
                FxaServer.Release
            }
            return ServerConfig(releaseServer, CLIENT_ID, REDIRECT_URL, tokenServerOverride)
        }
        return ServerConfig(FxaServer.Custom(serverOverride), CLIENT_ID, REDIRECT_URL, tokenServerOverride)
    }
}
