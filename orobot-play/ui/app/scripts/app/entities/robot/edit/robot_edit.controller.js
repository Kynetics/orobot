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

angular.module('angularApp').controller('RobotEditController', ["$scope", "Robot", "ngToast", "Log", "$state", function ($scope, Robot, ngToast, Log, $state) {


  var self = this;

  self.customer = {};
  self.asdress = {};


  self.clear = function () {
    self.customer = {};
    self.address = {};
  };

  self.save = function () {
    self.customer.address = self.address;
    Log.debug("Request:" + angular.toJson(self.customer));
    Robot.create(self.customer).then(function success() {
      ngToast.create(new Date().toLocaleTimeString().toString() + " - oRobot saved");
      $state.go("entity.robot_search");
    }, function error() {
      ngToast.danger(new Date().toLocaleTimeString().toString() + " - Error creating robot");
    });
  };

}]);
