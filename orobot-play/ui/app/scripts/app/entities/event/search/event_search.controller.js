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

angular.module('angularApp').controller('EventSearchController', ["$scope", "Event", "PER_PAGE", "$state", "Log", function ($scope, Event, PER_PAGE, $state, Log) {


  var self = this;

  $scope.page = 1;
  $scope.per_page = PER_PAGE;
  self.events = [];
  self.filter = {};
  if ($state.params.uuid) {
    self.filter.uuid = $state.params.uuid;
  }

  $scope.options = {
    rowHeight: 50,
    columnMode: 'flex',
    headerHeight: 50,
    footerHeight: false,
    scrollbarV: false,
    selectable: false
  };

  $scope.loadPage = function (page) {
    Log.debug("EventSearchController, loadPage: " + page);
    $scope.page = page;
    self.search();
  };

  self.search = function () {
    if (self.filter.uuid) {
      Event.getByRobotId(self.filter.uuid).then(function (response) {
        self.events =response.data;
      });
    } else {
      Event.getAllEvents(1, 100).then(function (response) {
        self.events = response.data;
      })
    }
  };

  self.clear = function () {
    self.filter = {};
  };

  self.search();


  self.getRequest = function () {
    /*Log.debug("EventSearchController, getRequest with :" + $scope.page + ", " + $scope.per_page + ", " + angular.toJson(self.filter));*/
    var request = {};
    request.page = $scope.page;
    request.per_page = $scope.per_page;

    return request;
  };

  self.showEventDetail = function (eventId) {
    $state.go("entity.event_detail", {
      "eventId": eventId
    });
  };


}]);
