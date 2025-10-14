import {
  createCanvas,
  getMouseCoordinates,
  isCanvasSupported,
} from "../helpers/utils";

function EventManager(chart) {
  this.chart = chart;
  this.lastObjectId = 0;
  this.objectMap = [];
  this.rectangularRegionEventSubscriptions = [];
  this.previousDataPointEventObject = null;

  this.ghostCanvas = createCanvas(this.chart.width, this.chart.height);
  this.ghostCtx = this.ghostCanvas.getContext("2d");

  this.mouseoveredObjectMaps = [];
}

EventManager.prototype.reset = function () {
  this.lastObjectId = 0;
  this.objectMap = [];
  this.rectangularRegionEventSubscriptions = [];
  this.previousDataPointEventObject = null;

  this.eventObjects = [];

  if (isCanvasSupported) {
    this.ghostCtx.clearRect(0, 0, this.chart.width, this.chart.height);
    this.ghostCtx.beginPath();
  }
};

EventManager.prototype.getNewObjectTrackingId = function () {
  return ++this.lastObjectId;
};

EventManager.prototype.mouseEventHandler = function (ev) {
  if (ev.type !== "mousemove" && ev.type !== "click") return;

  var eventObjectMaps = [];
  var xy = getMouseCoordinates(ev);
  var id = null;

  id = this.chart.getObjectAtXY(xy.x, xy.y, false);

  if (id && typeof this.objectMap[id] !== "undefined") {
    var eventObjectMap = this.objectMap[id];

    if (eventObjectMap.objectType === "dataPoint") {
      var dataSeries = this.chart.data[eventObjectMap.dataSeriesIndex];
      var dataPoint = dataSeries.dataPoints[eventObjectMap.dataPointIndex];
      var dataPointIndex = eventObjectMap.dataPointIndex;

      eventObjectMap.eventParameter = {
        x: xy.x,
        y: xy.y,
        dataPoint: dataPoint,
        dataSeries: dataSeries._options,
        dataPointIndex: dataPointIndex,
        dataSeriesIndex: dataSeries.index,
        chart: this.chart._publicChartReference,
      };
      eventObjectMap.eventContext = {
        context: dataPoint,
        userContext: dataPoint,
        mouseover: "mouseover",
        mousemove: "mousemove",
        mouseout: "mouseout",
        click: "click",
      };
      eventObjectMaps.push(eventObjectMap);

      eventObjectMap = this.objectMap[dataSeries.id];

      eventObjectMap.eventParameter = {
        x: xy.x,
        y: xy.y,
        dataPoint: dataPoint,
        dataSeries: dataSeries._options,
        dataPointIndex: dataPointIndex,
        dataSeriesIndex: dataSeries.index,
        chart: this.chart._publicChartReference,
      };

      eventObjectMap.eventContext = {
        context: dataSeries,
        userContext: dataSeries._options,
        mouseover: "mouseover",
        mousemove: "mousemove",
        mouseout: "mouseout",
        click: "click",
      };
      eventObjectMaps.push(this.objectMap[dataSeries.id]);
    } else if (eventObjectMap.objectType === "legendItem") {
      var dataSeries = this.chart.data[eventObjectMap.dataSeriesIndex];
      var dataPoint =
        eventObjectMap.dataPointIndex !== null
          ? dataSeries.dataPoints[eventObjectMap.dataPointIndex]
          : null;

      eventObjectMap.eventParameter = {
        x: xy.x,
        y: xy.y,
        dataSeries: dataSeries._options,
        dataPoint: dataPoint,
        dataPointIndex: eventObjectMap.dataPointIndex,
        dataSeriesIndex: eventObjectMap.dataSeriesIndex,
        chart: this.chart._publicChartReference,
      };
      eventObjectMap.eventContext = {
        context: this.chart.legend,
        userContext: this.chart.legend._options,
        mouseover: "itemmouseover",
        mousemove: "itemmousemove",
        mouseout: "itemmouseout",
        click: "itemclick",
      };
      eventObjectMaps.push(eventObjectMap);
    }
  }

  var mouseOutObjectMapsExcluded = [];
  for (var i = 0; i < this.mouseoveredObjectMaps.length; i++) {
    var mouseOut = true;

    for (var j = 0; j < eventObjectMaps.length; j++) {
      if (eventObjectMaps[j].id === this.mouseoveredObjectMaps[i].id) {
        mouseOut = false;
        break;
      }
    }

    if (mouseOut) {
      this.fireEvent(this.mouseoveredObjectMaps[i], "mouseout", ev);
    } else {
      mouseOutObjectMapsExcluded.push(this.mouseoveredObjectMaps[i]);
    }
  }

  this.mouseoveredObjectMaps = mouseOutObjectMapsExcluded;

  for (var i = 0; i < eventObjectMaps.length; i++) {
    var existing = false;

    for (var j = 0; j < this.mouseoveredObjectMaps.length; j++) {
      if (eventObjectMaps[i].id === this.mouseoveredObjectMaps[j].id) {
        existing = true;
        break;
      }
    }

    if (!existing) {
      this.fireEvent(eventObjectMaps[i], "mouseover", ev);
      this.mouseoveredObjectMaps.push(eventObjectMaps[i]);
    }

    if (ev.type === "click") {
      this.fireEvent(eventObjectMaps[i], "click", ev);
    } else if (ev.type === "mousemove") {
      this.fireEvent(eventObjectMaps[i], "mousemove", ev);
    }
  }
};

EventManager.prototype.fireEvent = function (eventObjectMap, eventType, ev) {
  if (!eventObjectMap || !eventType) return;

  var eventParameter = eventObjectMap.eventParameter;
  var eventContext = eventObjectMap.eventContext;
  var userContext = eventObjectMap.eventContext.userContext;

  if (userContext && eventContext && userContext[eventContext[eventType]])
    userContext[eventContext[eventType]].call(userContext, eventParameter);

  if (eventType !== "mouseout") {
    if (userContext.cursor && userContext.cursor !== ev.target.style.cursor) {
      ev.target.style.cursor = userContext.cursor;
    }
  } else {
    ev.target.style.cursor = this.chart._defaultCursor;
    delete eventObjectMap.eventParameter;
    delete eventObjectMap.eventContext;
  }

  if (
    eventType === "click" &&
    eventObjectMap.objectType === "dataPoint" &&
    this.chart.pieDoughnutClickHandler
  ) {
    this.chart.pieDoughnutClickHandler.call(
      this.chart.data[eventObjectMap.dataSeriesIndex],
      eventParameter,
    );
  }
};

export default EventManager;
