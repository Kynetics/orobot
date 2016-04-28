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

angular.module('angularApp').factory('AuthServerProvider', ["$http", "localStorageService", "$window", "Log", function loginService($http, localStorageService, $window, Log) {
  return {
    login: function (credentials) {
      var data = {email: credentials.username, password: credentials.password, rememberMe: credentials.rememberMe};
      return $http.post('/signIn', data).success(function (response) {
        Log.debug("*************************** Token: " + response.token);
        localStorageService.set('token', response.token);
        return response;
      });
    },
    logout: function () {
      // logout from the server
      $http.post('/signOut').success(function (response) {
        localStorageService.clearAll();
        // to get a new csrf token call the api
        $http.get('/user');
        return response;
      });
    },
    getToken: function () {
      var token = localStorageService.get('token');
      return token;
    },
    hasValidToken: function () {
      var token = this.getToken();
      return !!token;
    }
  };
}]);
