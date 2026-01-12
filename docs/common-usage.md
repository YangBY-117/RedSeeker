# Common 模块使用说明

本文档说明通用响应与异常处理的使用方式。

## ApiResponse

`ApiResponse<T>` 是统一的接口响应包装。

成功返回：

```java
return ApiResponse.ok(data);
```

自定义成功信息：

```java
return ApiResponse.ok("创建成功", data);
```

失败返回（通常由异常层处理）：

```java
throw new ServiceException(ErrorCode.VALIDATION_ERROR, "city 是必填项");
```

响应字段：

- `success`：是否成功
- `message`：提示信息
- `data`：成功数据或错误详情
- `code`：错误码（仅失败时存在）
- `timestamp`：服务端生成时间

## ErrorCode

使用 `ErrorCode` 统一描述错误类别与 HTTP 状态。

```java
throw new ServiceException(ErrorCode.NOT_FOUND, "景点不存在");
```

内置错误码：

- `VALIDATION_ERROR` -> 400
- `NOT_FOUND` -> 404
- `INTERNAL_ERROR` -> 500

## ServiceException

`ServiceException` 是业务异常，包含：

- `ErrorCode`：错误码与 HTTP 状态
- `message`：返回给客户端的信息
- `details`：可选的结构化错误详情

示例：

```java
throw new ServiceException(ErrorCode.VALIDATION_ERROR, "userId 是必填项");
```

```java
Map<String, Object> details = Map.of("field", "city");
throw new ServiceException(ErrorCode.VALIDATION_ERROR, "city 是必填项", details);
```

## GlobalExceptionHandler

`GlobalExceptionHandler` 统一将异常转换为 `ApiResponse`。

- `ServiceException` 根据其 `ErrorCode` 返回对应 HTTP 状态。
- 参数校验失败返回 `VALIDATION_ERROR`。
- 未捕获异常返回 `INTERNAL_ERROR`。
