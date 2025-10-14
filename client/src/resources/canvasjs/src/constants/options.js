export const isDebugMode = false;

export const isCanvasSupported = !!document.createElement("canvas").getContext;

export const defaultOptions = {
  Chart: {
    width: 500,
    height: 400,
    zoomEnabled: false,
    zoomType: "x",
    backgroundColor: "white",
    theme: "theme1",
    animationEnabled: false,
    animationDuration: 1200,
    dataPointMaxWidth: null,

    colorSet: "colorSet1",
    culture: "en",
    creditText: "CanvasJS.com",
    interactivityEnabled: true,
    exportEnabled: false,
    exportFileName: "Chart",

    rangeChanging: null,
    rangeChanged: null,
  },

  Title: {
    padding: 0,
    text: null,
    verticalAlign: "top",
    horizontalAlign: "center",
    fontSize: 20,
    fontFamily: "Calibri",
    fontWeight: "normal",
    fontColor: "black",
    fontStyle: "normal",

    borderThickness: 0,
    borderColor: "black",
    cornerRadius: 0,
    backgroundColor: null,
    margin: 5,
    wrap: true,
    maxWidth: null,

    dockInsidePlotArea: false,
  },

  Subtitle: {
    padding: 0,
    text: null,
    verticalAlign: "top",
    horizontalAlign: "center",
    fontSize: 14,
    fontFamily: "Calibri",
    fontWeight: "normal",
    fontColor: "black",
    fontStyle: "normal",

    borderThickness: 0,
    borderColor: "black",
    cornerRadius: 0,
    backgroundColor: null,
    margin: 2,
    wrap: true,
    maxWidth: null,

    dockInsidePlotArea: false,
  },

  Legend: {
    name: null,
    verticalAlign: "center",
    horizontalAlign: "right",

    fontSize: 14,
    fontFamily: "calibri",
    fontWeight: "normal",
    fontColor: "black",
    fontStyle: "normal",

    cursor: null,
    itemmouseover: null,
    itemmouseout: null,
    itemmousemove: null,
    itemclick: null,

    dockInsidePlotArea: false,
    reversed: false,

    maxWidth: null,
    maxHeight: null,

    itemMaxWidth: null,
    itemWidth: null,
    itemWrap: true,
    itemTextFormatter: null,
  },

  ToolTip: {
    enabled: true,
    shared: false,
    animationEnabled: true,
    content: null,
    contentFormatter: null,

    reversed: false,

    backgroundColor: null,

    borderColor: null,
    borderThickness: 2,
    cornerRadius: 5,

    fontSize: 14,
    fontColor: "#000000",
    fontFamily: "Calibri, Arial, Georgia, serif;",
    fontWeight: "normal",
    fontStyle: "italic",
  },

  Axis: {
    minimum: null,
    maximum: null,
    viewportMinimum: null,
    viewportMaximum: null,
    interval: null,
    intervalType: null,

    title: null,
    titleFontColor: "black",
    titleFontSize: 20,
    titleFontFamily: "arial",
    titleFontWeight: "normal",
    titleFontStyle: "normal",

    labelAngle: 0,
    labelFontFamily: "arial",
    labelFontColor: "black",
    labelFontSize: 12,
    labelFontWeight: "normal",
    labelFontStyle: "normal",
    labelAutoFit: false,
    labelWrap: true,
    labelMaxWidth: null,
    labelFormatter: null,

    prefix: "",
    suffix: "",

    includeZero: true,

    tickLength: 5,
    tickColor: "black",
    tickThickness: 1,

    lineColor: "black",
    lineThickness: 1,
    lineDashType: "solid",

    gridColor: "A0A0A0",
    gridThickness: 0,
    gridDashType: "solid",

    interlacedColor: null,

    valueFormatString: null,

    margin: 2,

    stripLines: [],
  },

  StripLine: {
    value: null,
    startValue: null,
    endValue: null,

    color: "orange",
    opacity: null,
    thickness: 2,
    lineDashType: "solid",
    label: "",
    labelBackgroundColor: "#EEEEEE",
    labelFontFamily: "arial",
    labelFontColor: "orange",
    labelFontSize: 12,
    labelFontWeight: "normal",
    labelFontStyle: "normal",
    labelFormatter: null,

    showOnTop: false,
  },

  DataSeries: {
    name: null,
    dataPoints: null,
    label: "",
    bevelEnabled: false,
    highlightEnabled: true,

    cursor: null,

    indexLabel: "",
    indexLabelPlacement: "auto",
    indexLabelOrientation: "horizontal",
    indexLabelFontColor: "black",
    indexLabelFontSize: 12,
    indexLabelFontStyle: "normal",
    indexLabelFontFamily: "Arial",
    indexLabelFontWeight: "normal",
    indexLabelBackgroundColor: null,
    indexLabelLineColor: null,
    indexLabelLineThickness: 1,
    indexLabelLineDashType: "solid",
    indexLabelMaxWidth: null,
    indexLabelWrap: true,
    indexLabelFormatter: null,

    lineThickness: 2,
    lineDashType: "solid",

    color: null,
    risingColor: "white",
    fillOpacity: null,

    startAngle: 0,

    radius: null,
    innerRadius: null,

    type: "column",
    xValueType: "number",
    axisYType: "primary",

    xValueFormatString: null,
    yValueFormatString: null,
    zValueFormatString: null,
    percentFormatString: null,

    showInLegend: null,
    legendMarkerType: null,
    legendMarkerColor: null,
    legendText: null,
    legendMarkerBorderColor: null,
    legendMarkerBorderThickness: null,

    markerType: "circle",
    markerColor: null,
    markerSize: null,
    markerBorderColor: null,
    markerBorderThickness: null,
    mouseover: null,
    mouseout: null,
    mousemove: null,
    click: null,
    toolTipContent: null,

    visible: true,
  },

  TextBlock: {
    x: 0,
    y: 0,
    width: null,
    height: null,
    maxWidth: null,
    maxHeight: null,
    padding: 0,
    angle: 0,
    text: "",
    horizontalAlign: "center",
    fontSize: 12,
    fontFamily: "calibri",
    fontWeight: "normal",
    fontColor: "black",
    fontStyle: "normal",

    borderThickness: 0,
    borderColor: "black",
    cornerRadius: 0,
    backgroundColor: null,
    textBaseline: "top",
  },

  CultureInfo: {
    decimalSeparator: ".",
    digitGroupSeparator: ",",
    zoomText: "Zoom",
    panText: "Pan",
    resetText: "Reset",

    menuText: "More Options",
    saveJPGText: "Save as JPG",
    savePNGText: "Save as PNG",

    days: [
      "Sunday",
      "Monday",
      "Tuesday",
      "Wednesday",
      "Thursday",
      "Friday",
      "Saturday",
    ],
    shortDays: ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"],

    months: [
      "January",
      "February",
      "March",
      "April",
      "May",
      "June",
      "July",
      "August",
      "September",
      "October",
      "November",
      "December",
    ],
    shortMonths: [
      "Jan",
      "Feb",
      "Mar",
      "Apr",
      "May",
      "Jun",
      "Jul",
      "Aug",
      "Sep",
      "Oct",
      "Nov",
      "Dec",
    ],
  },
};
