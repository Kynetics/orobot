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

angular.module('angularApp').config(["$stateProvider", function ($stateProvider) {
  $stateProvider.state('entity.robot_search', {
    url: '/robot/search',
    data: {
      roles: [],
      pageTitle: 'robot.search.title'
    },
    views: {
      'title@': {
        templateUrl: 'scripts/components/title/title.html',
        controller: 'TitleController',
        controllerAs: 'TitleCtrl'
      },
      'content@entity': {
        templateUrl: 'scripts/app/entities/robot/search/robot_search.html',
        controller: 'RobotSearchController',
        controllerAs: 'RobotSearchCtrl'
      }
    },
    resolve: {
      robotTranslatePartialLoader: ['$translate', '$translatePartialLoader', function ($translate, $translatePartialLoader) {
        $translatePartialLoader.addPart('robot');
        return $translate.refresh();
      }]
    }
  });
}]);
