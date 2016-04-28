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

angular.module('angularApp').controller('RobotSearchController', ["$scope", "Robot", "PER_PAGE", "$state","Log", function ($scope, Robot, PER_PAGE, $state, Log) {


  var self = this;

  $scope.page = 1;
  $scope.per_page = PER_PAGE;
  self.filter = {};

  self.robots = [];

  $scope.options = {
    rowHeight: 50,
    columnMode: 'flex',
    headerHeight: 50,
    footerHeight: false,
    scrollbarV: false,
    selectable: false
  };

  $scope.loadPage = function (page) {
    Log.debug("RobotSearchController, loadPage: " + page);
    $scope.page = page;
    self.search();
  };

  self.search = function () {
    Robot.getAll(1, 100).then(function (response) {
      self.robots = response.data;
    });
  };

  self.search();

  self.clear = function () {
    self.filter = {};
    self.search();
  };

  self.getRequest = function () {
    /*Log.debug("RobotSearchController, getRequest with :" + $scope.page + ", " + $scope.per_page + ", " + angular.toJson(self.filter));*/
    var request = {};
    request.page = $scope.page;
    request.per_page = $scope.per_page;

    return request;
  };

  self.showRobotDetail = function (robotId) {
    $state.go("entity.robot_detail", {
      "robotId" : robotId
    });
  };


}]);
