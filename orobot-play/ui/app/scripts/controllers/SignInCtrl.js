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

/*global app: false */

/**
 * The sign in controller.
 */
app.controller('SignInCtrl', ['$scope', '$alert', '$auth', function($scope, $alert, $auth) {

  /**
   * Submits the login form.
   */
  $scope.submit = function() {
    $auth.setStorage($scope.rememberMe ? 'localStorage' : 'sessionStorage');
    $auth.login({ email: $scope.email, password: $scope.password, rememberMe: $scope.rememberMe })
      .then(function() {
        $alert({
          content: 'You have successfully signed in',
          animation: 'fadeZoomFadeDown',
          type: 'material',
          duration: 3
        });
      })
      .catch(function(response) {
        console.log(response);
        $alert({
          content: response.data.message,
          animation: 'fadeZoomFadeDown',
          type: 'material',
          duration: 3
        });
      });
  };


}]);
