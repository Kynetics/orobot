/*
* Copyright 2015 Kynetics SRL
*
* This file is part of orobot.
*
* orobot is free software: you can redistribute it and/or modify
* it under the terms of the GNU Affero General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* orobot is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU Affero General Public License for more details.
*
* You should have received a copy of the GNU Affero General Public License
* along with orobot.  If not, see <http://www.gnu.org/licenses/>.
*/
'use strict';

angular.module('angularApp')
  .factory('WebsocketService', ["AuthServerProvider", "Log", function WebsocketService(AuthServerProvider, Log) {
    var _ws;

    return {
      startConnection: function (onOpen, onMessageReceived) {
        if ("WebSocket" in window) {
          Log.debug("WebSocket is supported by your Browser!");

          // Let us open a web socket
          _ws = new WebSocket("ws://localhost:9000/wsconnect?token=" + AuthServerProvider.getToken());

          _ws.onopen = onOpen;

          _ws.onmessage = function (evt) {
            var received_msg = angular.fromJson(evt.data);
            Log.debug("WebsocketService, message received:" + received_msg);
            onMessageReceived(received_msg);
          };
        }
        else {
          // The browser doesn't support WebSocket
          Log.debug("WebsocketService, WebSocket NOT supported by your Browser!");
        }
      },

      closeConnection: function (onClose) {
        onClose();
        _ws.close();
        Log.debug("WebsocketService, Connection is closed...");
      },

      isConnected: function () {
        var connected = false;
        if (typeof _ws !== 'undefined' && _ws.readyState == 1) {
          Log.debug("WebsocketService, WebSocket status: " + _ws.readyState);
          connected = true;
        }
        return connected;
      },

      send: function (message) {
        if (this.isConnected()) {
          var messageToSend = angular.toJson(message);
          Log.debug("WebsocketService, Message to send: " + messageToSend);
          _ws.send(messageToSend);
          Log.debug("WebsocketService, Message sent");
          Log.debug("WebsocketService, WebSocket status: " + _ws.readyState);
        }
        else {
          Log.debug("WebsocketService, can't send message - not connected");
        }

      }

    }
  }]);
