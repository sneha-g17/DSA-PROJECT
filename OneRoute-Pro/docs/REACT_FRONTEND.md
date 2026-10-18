# React frontend

The interface is built with React and bundled using esbuild. The Java server serves the resulting static files. Node.js is a build/test tool, not the application backend.

| File | What to explain |
|---|---|
| main.jsx | createRoot mounts App in the HTML root element |
| App.jsx | useState stores data and form values; useEffect loads the network; useRef stores request cancellation and the dialog |
| JourneyForm.jsx | Controlled inputs receive values and callbacks through props |
| NetworkMap.jsx | SVG elements are rendered from graph data; useMemo avoids rebuilding unchanged selections; useState controls zoom |
| JourneyCards.jsx | Route props become cards and grouped itineraries |
| helpers.js | Formatting, station lookup and saved-journey validation |

A user changes an input → its onChange callback updates App state → React renders the new value. On submit, App sends JSON to Java. Java calls the C++ engine and returns route JSON. Updating response state makes React render route cards and highlight the selected map path.

React escapes displayed station text. Favourites are stored only in this browser. Example trips reset the filters, making them predictable demonstration starting points.

Run npm ci once in frontend, then npm run build after editing source. Next rebuild the backend from the root with bash build.sh (or build.bat): Spring serves the frontend packaged inside its JAR. Do not edit generated web/app.js directly. The pinned package lock is included; node_modules is excluded from the ZIP.

References: [React createRoot](https://react.dev/reference/react-dom/client/createRoot), [esbuild build API](https://esbuild.github.io/api/#build).
