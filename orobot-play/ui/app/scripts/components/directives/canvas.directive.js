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

angular.module("angularApp").directive('drawMap', [function () {
  return function (scope, element, attrs, controller) {
    var markX = scope.$eval(attrs.mapMarkX);
    var markY = scope.$eval(attrs.mapMarkY);
    var markAngle = scope.$eval(attrs.mapMarkAngle);
    var markSrc = attrs.mapMarkSrc;

    var canvas = element[0];
    var ctx = canvas.getContext("2d");

    var markImage = new Image();
    markImage.onload = function () {
      ctx.rotate(markAngle * Math.PI / 180);
      ctx.drawImage(markImage, markX, markY);
    }
    markImage.src = markSrc;

    var updateMark = function (x, y, angle) {
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      ctx.rotate(angle * Math.PI / 180);
      ctx.drawImage(markImage, x, y);
    }

  };
}]);
