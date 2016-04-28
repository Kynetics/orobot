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

angular.module("angularApp").controller("GlobalController", ["Log", "$scope", "$mdSidenav", "$mdMedia","Principal", function (Log, $scope,  $mdSidenav, $mdMedia,Principal) {

  var self = this;
  self.isAuthenticated = function () {
    return Principal.isAuthenticated();
  };
  self.title = "No title";
  // Funzioni di utilità globale
  self.showErrors = function (myForm, fieldName) {
    return (myForm.$submitted || myForm[fieldName].$dirty) && myForm[fieldName].$invalid;
  };

  $scope.$mdMedia = $mdMedia;

  $mdSidenav('left').then(function (instance) {
    Log.debug("Sidebar left is now ready");
  });

  self.openLeftMenu = function () {
    Log.debug("openLefMenu, enter");
    $mdSidenav('left').toggle().then(function () {
    });
  };


  self.capitalizeOnlyFirstChar = function capitalizeOnlyFirstChar(string) {
    string = string.toLowerCase();
    var pieces = string.split(/\s\s+/g);
    for (var i = 0; i < pieces.length; i++) {
      var j = pieces[i].charAt(0).toUpperCase();
      pieces[i] = j + pieces[i].substr(1);
    }
    return pieces.join(" ");
  };

}]);
