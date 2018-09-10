# Соглашение

*При нажатии на кнопку* **Search** *происходит следующее:*
 1. Отправляется **GET** запрос c Клиент -> Сервер. Параметры запроса передаются в Request URL в приведенном формате:

    {- http://hostname:port/routes?dateStart={yyyy-mm-dd}&region={int}&maxPoints={int}&maxCars={int}&newDate={yyyy-mm-dd}&optType={int}&optAlg={int} -}

    The only strictly requied parameter is **dateStart** which indicates on which date search must be performed.
    All other parameters have default values, they must be provided if needed.
    
 2. Отправляется Response **POST** Сервер -> Клиент. Response имеет следующую форму:
```json
[
    {
        "n": -999,
        "region": 43,
        "fdate": "2018-04-26T21:00:00.000+0000",
        "changed": false,
        "points": 1,
        "length": 0,
        "time": 0,
        "cost": 0,
        "costCurr": "NOT_DEFINED",
        "status": "CREATED",
        "resultFlag": 0,
        "editable": true,
        "approveable": true
    },
    {
        "n": 2445,
        "region": 43,
        "fdate": "2018-04-26T21:00:00.000+0000",
        "changed": false,
        "points": 6,
        "length": 12337,
        "time": 22,
        "cost": 925,
        "costCurr": "USD",
        "status": "CREATED",
        "resultFlag": 0,
        "editable": true,
        "approveable": true
    }
]    
```

*Загрузка страницы*
 1. Отправляется Request **GET** запрос c Клиент -> Сервер для получения информации о списке Carriers. Запрос для получения всех Carriers имеет следующую форму:

    {- http://hostname:port/routes/carriers -}

 2. Отправляется Response **POST** Сервер -> Клиент. Response имеет следующую форму:
```json
[
    {
        "id": 43,
        "name": "Internal",
        "descx": "Banks's own armored service",
        "instId": "9999",
        "instDescx": null,
        "depotId": "15",
        "depotDescx": null
    }
]
```

*При нажатии на кнопку* **один из доступных маршрутов** *происходит следующее:*

 1. Отправляется Request **GET** запрос c Клиент -> Сервер для получения информации о списке Points. Запрос для получения всех Points для маршрута имеющего {id} имеет следующую форму:
    * Non-default route
	
         {- http://hostname:port/routes/{id}/points -}
	* Default route
	
	    {- http://hostname:port/routes/DEFAULT_ROUTE_NUMBER/points/?dateStart={yyyy-mm-dd}&region={int}&maxPoints={int}&maxCars={int}&newDate={yyyy-mm-dd}&optType={int}&optAlg={int} -}

        The only strictly requied parameter is **dateStart** which indicates on which date search must be performed.
        All other parameters have default values, they must be provided if needed. 
	    DEFAULT_ROUTE_NUMBER = -999
	    
 2. Отправляется Response **POST** Сервер -> Клиент. Response имеет следующую форму:
```json
[
    {
        "n": 1,
        "pid": "15",
        "encID": 0,
        "arrivalTime": 0,
        "latitude": "33.1285979",
        "longitude": "-96.82637899999997",
        "adress": "Frisco, 6713 Massa Lane",
        "visited": false,
        "reorder": false,
        "depot": true,
        "broken": false
    },
    {
        "n": 2,
        "pid": "155750",
        "encID": 106194,
        "arrivalTime": 6,
        "latitude": "33.115825",
        "longitude": "-96.84041388888889",
        "adress": "Frisco, 5401 Lebanon",
        "visited": false,
        "reorder": false,
        "depot": false,
        "broken": false
    }
]
```

*Add и Delete маршрут:*

 1. Для добавление маршрута отправляется **POST** Request Клиент -> Сервер со следующим Body:
```json
{
	"dateStart" : "2018-04-27",
	"region" : 43
}
```

    Если маршрут успешно добавлен, то будет возвращен следующий Response:
    ```
        Content-length: 0
        http_status: 201(CREATED)
    ```

 2. Для удаления маршрута отправляется **DELETE** Request Клиент -> Сервер со следующими Request URL и Body соответственно:

    {- http://hostname:port/routes/{id} -}

    ```json
    {
    "dateStart" : "2018-04-27",
	"region" : 43
    }
    ```

    Если маршрут успешно добавлен, то будет возвращен следующий Response:
    ```
        Content-length: 0
        http_status: 204(NO_CONTENT)
    ```

**Approve** *маршрут*

 1. Для Approve маршрута отправляется **PATCH** Request Клиент -> Сервер со следующим URL:
    
    {- http://hostname:port/routes/{id}/approve -}
 
   Если маршрут успешно Approve, то будет возвращен следующий Response:
 
     ```json
        {
         "n": {id},
         "region": 43,
         "fdate": "2018-04-26T21:00:00.000+0000",
         "changed": false,
         "points": 6,
         "length": 12337,
         "time": 22,
         "cost": 925,
         "costCurr": "USD",
         "status": "APPROVED",
         "resultFlag": 0,
         "editable": true,
         "approveable": true
     }
     ```
     ```
         http_status: 200(OK)
     ```

**Recalculate** *маршрут*
 1. Для Recalculate маршрута отправляется **PATCH** Request Клиент -> Сервер по следующему URL и со следующим Body:
    
    {- http://hostname:port/routes/{id}/recalculate -}

    ```json
    {
    "dateStart": date.format(YYYY-MM-DD),
    "region": 43,
    "maxPoints": 30,
    "maxCars": 6,
    "newDate": date.format(YYYY-MM-DD)
    "routingType": "CostOpt = 1" | "ElapsedTimeOpt = 2" | "ComplexOpt = 3" 
    "optimizationType": "Genetic = 1" | "Ants = 2" | "Pareto = 3" | "VNS = 4"
    }
    ```
    ```
    region = carrier
    dateStart = route date
    routingType = optType
    optimizationType = optAlgo
    ```

    **Note**: There are NO DEFAULTS on server side here, so client must pass all fields using defaults for not needed fields. The         defaults are:
    * REGION = 43
    * MAX_POINTS = 30
    * MAX_CARS = 6
    * NEW_DATE = "2000-01-01"
    * ROUING_TYPE = 1
    * OPTIMIZATION_TYPE = 1
    * There is NO DEFAULT for dateStart, so it must be always set.
    
 2. После Recalculate будет возвращен следующий Response:
     ```json
        {
         "n": {id},
         "region": 43,
         "fdate": "2018-04-26T21:00:00.000+0000",
         "changed": false,
         "points": 6,
         "length": 12337,
         "time": 22,
         "cost": 925,
         "costCurr": "USD",
         "status": "APPROVED",
         "resultFlag": 0,
         "editable": true,
         "approveable": true
     }
     ```
     ```
         http_status: 200(OK)
     ```

*При нажатии на кнопку* **Calculate** *происходит следующее:*
 1. Отправляется **POST** запрос c Клиент -> Сервер. Параметры запроса передаются в Request Body в приведенном формате:
    ```json
    {
    "dateStart": date.format(YYYY-MM-DD),
    "region": 43,
    "maxPoints": 30,
    "maxCars": 6,
    "newDate": date.format(YYYY-MM-DD)
    "routingType": "CostOpt = 1" | "ElapsedTimeOpt = 2" | "ComplexOpt = 3" 
    "optimizationType": "Genetic = 1" | "Ants = 2" | "Pareto = 3" | "VNS = 4"
    }
    ```
    ```
    region = carrier
    dateStart = route date
    routingType = optType
    optimizationType = optAlgo
    ```

    **Note**: There are NO DEFAULTS on server side here, so client must pass all fields using defaults for not needed fields. The         defaults are:
    * REGION = 43
    * MAX_POINTS = 30
    * MAX_CARS = 6
    * NEW_DATE = "2000-01-01"
    * ROUING_TYPE = 1
    * OPTIMIZATION_TYPE = 1
    There is NO DEFAULT for dateStart, so it must be always set.

 2. Отправляется Response **POST** Сервер -> Клиент. Response имеет следующую форму(аналогичен Response для GetRoutes, т.е.
 передаются все маршруты, но с новыми, после Calculate, данными):
    ```json
    [
        {
        "n": -999,
        "region": 43,
        "fdate": "2018-04-26T21:00:00.000+0000",
        "changed": false,
        "points": 1,
        "length": 0,
        "time": 0,
        "cost": 0,
        "costCurr": "NOT_DEFINED",
        "status": "CREATED",
        "resultFlag": 0,
        "editable": true,
        "approveable": true
        },
        {
        "n": 2445,
        "region": 43,
        "fdate": "2018-04-26T21:00:00.000+0000",
        "changed": false,
        "points": 6,
        "length": 12337,
        "time": 22,
        "cost": 925,
        "costCurr": "USD",
        "status": "CREATED",
        "resultFlag": 0,
        "editable": true,
        "approveable": true
        }
    ]    
    ```
    ```
        http_status: 200(OK)
    ```


*Для* **гео-кодирования** *точек на отдельной вкладке:*

 1. Отправляется Request **PATCH** Клиент -> Сервер. Request имеет следующие URL и Body:

    {- http://hostname:port/routes/geocode/atms -}

     ```json
     [
         {
             "n": 1,
             "pid": "15",
             "encID": 0,
             "arrivalTime": 0,
             "latitude": not_set,
             "longitude": not_set,
             "adress": "Frisco, 6713 Massa Lane",
             "visited": false,
             "reorder": false,
             "depot": true,
             "broken": false
         },
         {
             "n": 2,
             "pid": "155750",
             "encID": 106194,
             "arrivalTime": 6,
             "latitude": not_set,
             "longitude": not_set,
             "adress": "Frisco, 5401 Lebanon",
             "visited": false,
             "reorder": false,
             "depot": false,
             "broken": false
         }
     ]
     ```
     **Note:** Request Body включает в себя точки загруженного маршрута.

 2. После geocode будет возвращен следующий Response:
     ```json
     [
         {
             "n": 1,
             "pid": "15",
             "encID": 0,
             "arrivalTime": 0,
             "latitude": "33.1285979",
             "longitude": "-96.82637899999997",
             "adress": "Frisco, 6713 Massa Lane",
             "visited": false,
             "reorder": false,
             "depot": true,
             "broken": false
         },
         {
             "n": 2,
             "pid": "155750",
             "encID": 106194,
             "arrivalTime": 6,
             "latitude": 0,
             "longitude": 0,
             "adress": "Frisco, 5401 Lebanon",
             "visited": false,
             "reorder": false,
             "depot": false,
             "broken": false
         }
     ]
     ```
     ```
        http_status: 200(OK)
     ```
     **Note:** Если после геокодирования некоторые точки вернулись с 0 координатами, то это следует трактовать как просьбу к               пользователю ввести координаты вручнную или проверить адрес.(ввод координат вручную не реализован)

# TODO 
* Ручной ввод координат
* Работа с точками маршрута
* **Spring JDBC** или **ORM**
* **Security**






