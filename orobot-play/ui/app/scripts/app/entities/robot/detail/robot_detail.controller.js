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

angular.module('angularApp').controller('RobotDetailController', ["$scope", "Robot", "ngToast", "Log", "$state", "AuthServerProvider", "WebsocketService", "easyrtc", "resolveRobot", function ($scope, Robot, ngToast, Log, $state, AuthServerProvider, WebsocketService, easyrtc, resolveRobot) {

  var self = this;

  self.robot = resolveRobot.data.state;
  self.socketUrl = resolveRobot.data.rtcSoketUrl;
  self.img = new Image();
  self.easyrtcid = -1;
  self.robot.easyrtcid = -1;
  self.isCalling = false;
  self.robot.videoOnline = false;


  if (self.robot.locationMap) {
    Robot.getMap(self.robot.locationMap).then(function (response) {
      Log.debug("response data: " + response.data);

      self.dataImage = "data:image/png;base64," + response.data;

      self.img.src = self.dataImage;

      Log.debug("img.src: " + self.img.src);

      /*self.img = Robot.getMap(self.robot.locationMap);*/
      Log.debug("locationMap: " + self.robot.locationMap);

      self.img.onload = function () {
        Log.debug("Image loaded");
        self.imgRenderedWidth = this.width;
        self.imgRenderedHeight = this.height;
        initMap(this.width, this.height);
        initCursor();
      };

      self.img.onerror = function (event) {
        Log.debug("Image error: " + angular.toJson(event));
      };
    });
  } else {
    Log.error("Map not found");

  }

  self.goToRobotEvent = function () {
    $state.go("entity.event", {
      "uuid": self.robot.uuid
    });
  };

  self.onOpen = function () {
    // Web Socket is connected, send data using send()
    var message = {
      'header': 'START_WATCHING',
      'payload': {
        'robotId': self.robot.uuid
      }
    };
    WebsocketService.send(message);
  };

  self.onClose = function () {
    var message = {
      'header': 'STOP_WATCHING'
    };
    WebsocketService.send(message);
  };

  Log.debug("token " + AuthServerProvider.getToken());

  self.startConnection = function () {
    WebsocketService.startConnection(self.onOpen, self.onMessageReceived);
  };

  self.closeConnection = function () {
    WebsocketService.closeConnection(self.onClose)
  };

  self.isConnected = function () {
    return WebsocketService.isConnected();
  };

  self.sendNewCoordinate = function (event) {
    self.positionX = event.offsetX;
    self.positionY = event.offsetY;
    var message = {
      'header': 'MOVETO',
      'payload': {
        'x': normalizePositionFromPx(self.positionX, self.imgRenderedWidth),
        'y': normalizePositionFromPx(self.positionY, self.imgRenderedHeight)
      }
    };
    WebsocketService.send(message);
  };

  self.normalizedPositionX = 0;
  self.normalizedPositionY = 0;

  self.mapMarkAngle = 0;
  self.rotate = function () {
    self.mapMarkAngle = (self.mapMarkAngle + 10) % 360;
  };

  self.cursor = document.getElementById('cursor');

  var initCursor = function initCursor() {
    self.cursor.style.position = 'absolute';
    self.cursor.style.top = '0px';
    self.cursor.style.left = '0px';
    self.cursor.style.height = '40px';
    self.cursor.style.width = '40px';
    self.cursor.style.visibility = 'visible';
  };


  var initMap = function (width, height) {
    var map = document.getElementById('map');
    map.style.width = width + 'px';
    map.style.height = height + 'px';
    map.style.backgroundImage = "url(" + self.img.src + ")";
    map.style.position = 'relative';
  };

  self.onMessageReceived = function (message) {
    var position = message.payload;
    Log.debug("onMessageReceived, position x, y, theta:" + angular.toJson(position));
    var event = {offsetX: position.x, offsetY: position.y, theta: position.h};
    $scope.$apply(function () {
      self.normalizedPositionX = event.offsetX;
      self.normalizedPositionY = event.offsetY;
      self.mapMarkAngle = radiansToDegrees(event.theta) + 90;
      self.cursor.style.top = (pxFromNormalizePosition(self.normalizedPositionY, self.imgRenderedHeight) - self.cursor.clientHeight / 2) + 'px';
      self.cursor.style.left = (pxFromNormalizePosition(self.normalizedPositionX, self.imgRenderedWidth) - self.cursor.clientWidth / 2) + 'px';
      self.cursor.style.rotation = self.mapMarkAngle;
    });
  };

  var radiansToDegrees = function (radians) {
    return (radians * 180) / Math.PI;
  };

  var pxFromNormalizePosition = function (normalizePosition, mapSideLength) {
    return normalizePosition * mapSideLength;
  };

  var normalizePositionFromPx = function (pxPosition, mapSideLength) {
    return pxPosition / mapSideLength;
  };


  // ====== EasyRTC Video call ======

  var startVideoConnection = function () {
    easyrtc.setSocketUrl(self.socketUrl);
    easyrtc.setVideoDims(640, 480);
    easyrtc.dontAddCloseButtons();
    easyrtc.setRoomOccupantListener(onRoomOccupant);
    easyrtc.easyApp("easyrtc.audioVideoSimple", "selfVideo", ["callerVideo"], loginSuccess, loginFailure);
  };

  var onRoomOccupant = function (roomName, data, isPrimary) {
    self.robot.easyrtcid = -1;
    self.robot.videoOnline = false;
    for (var easyrtcid in data) {
      var username = easyrtc.idToName(easyrtcid);
      if (username == "robot_" + self.robot.uuid) {
        self.robot.videoOnline = true;
        self.robot.easyrtcid = easyrtcid;
      }
    }
    $scope.$apply();
  };

  var loginSuccess = function (easyrtcid) {
    self.easyrtcid = easyrtcid;
    $scope.$apply();
  };


  var loginFailure = function (errorCode, message) {
    self.easyrtcid = -1;
    $scope.$apply();
    easyrtc.showError(errorCode, message);
  };


  self.performCall = function () {
    easyrtc.hangupAll();

    var successCB = function () {
      self.isCalling = true;
      $scope.$apply();
    };
    var failureCB = function () {
      self.isCalling = false;
      $scope.$apply();
    };
    easyrtc.call(self.robot.easyrtcid, successCB, failureCB);
  };

  self.endCall = function () {
    easyrtc.hangupAll();
    self.isCalling = false;
  };

  startVideoConnection();

  // END ====== EasyRTC Video call ======

}]);
