package com.telen.protocols.transport.socket

import com.telen.protocols.core.transport.ConnectionTarget

/**
 * A no-op marker: [SocketTransport] never holds a persistent connection open ahead of time, since
 * every command already carries its own address/port in its
 * [com.telen.protocols.core.model.RouteConfig] (a single device can use TCP for one command and UDP
 * for another, see [TcpRoute]/[UdpRoute]).
 */
object SocketTarget : ConnectionTarget
