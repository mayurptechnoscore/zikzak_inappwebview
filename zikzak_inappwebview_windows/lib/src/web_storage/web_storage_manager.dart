import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:zikzak_inappwebview_platform_interface/zikzak_inappwebview_platform_interface.dart';

/// Object specifying creation parameters for creating a [WindowsWebStorageManager].
///
/// When adding additional fields make sure they can be null or have a default
/// value to avoid breaking changes. See [PlatformWebStorageManagerCreationParams] for
/// more information.
@immutable
class WindowsWebStorageManagerCreationParams
    extends PlatformWebStorageManagerCreationParams {
  /// Creates a new [WindowsWebStorageManagerCreationParams] instance.
  const WindowsWebStorageManagerCreationParams(
    // This parameter prevents breaking changes later.
    // ignore: avoid_unused_constructor_parameters
    PlatformWebStorageManagerCreationParams params,
  ) : super();

  /// Creates a [WindowsWebStorageManagerCreationParams] instance based on [PlatformWebStorageManagerCreationParams].
  factory WindowsWebStorageManagerCreationParams.fromPlatformWebStorageManagerCreationParams(
    PlatformWebStorageManagerCreationParams params,
  ) {
    return WindowsWebStorageManagerCreationParams(params);
  }
}

///{@macro zikzak_inappwebview_platform_interface.PlatformWebStorageManager}
class WindowsWebStorageManager extends PlatformWebStorageManager
    with ChannelController {
  /// Creates a new [WindowsWebStorageManager].
  WindowsWebStorageManager(PlatformWebStorageManagerCreationParams params)
      : super.implementation(
          params is WindowsWebStorageManagerCreationParams
              ? params
              : WindowsWebStorageManagerCreationParams
                  .fromPlatformWebStorageManagerCreationParams(
                  params,
                ),
        ) {
    channel = const MethodChannel(
      'wtf.zikzak/zikzak_inappwebview_webstoragemanager',
    );
    handler = handleMethod;
    initMethodCallHandler();
  }

  static WindowsWebStorageManager? _instance;

  ///Gets the WebStorage manager shared instance.
  static WindowsWebStorageManager instance() {
    return (_instance != null) ? _instance! : _init();
  }

  static WindowsWebStorageManager _init() {
    _instance = WindowsWebStorageManager(
      WindowsWebStorageManagerCreationParams(
        const PlatformWebStorageManagerCreationParams(),
      ),
    );
    return _instance!;
  }

  Future<dynamic> _handleMethod(MethodCall call) async {}

  @override
  Future<List<WebsiteDataRecord>> fetchDataRecords({
    required Set<WebsiteDataType> dataTypes,
  }) async {
    List<WebsiteDataRecord> recordList = [];
    List<String> dataTypesList = [];
    for (var dataType in dataTypes) {
      dataTypesList.add(dataType.toNativeValue());
    }
    Map<String, dynamic> args = <String, dynamic>{};
    args.putIfAbsent("dataTypes", () => dataTypesList);
    List<Map<dynamic, dynamic>> records = (await channel?.invokeMethod<List>(
          'fetchDataRecords',
          args,
        ))
            ?.cast<Map<dynamic, dynamic>>() ??
        [];
    for (var record in records) {
      List<String> dataTypesString = record["dataTypes"].cast<String>();
      Set<WebsiteDataType> dataTypes = Set();
      for (var dataTypeValue in dataTypesString) {
        var dataType = WebsiteDataType.fromNativeValue(dataTypeValue);
        if (dataType != null) {
          dataTypes.add(dataType);
        }
      }
      recordList.add(
        WebsiteDataRecord(
          displayName: record["displayName"],
          dataTypes: dataTypes,
        ),
      );
    }
    return recordList;
  }

  @override
  Future<void> removeDataFor({
    required Set<WebsiteDataType> dataTypes,
    required List<WebsiteDataRecord> dataRecords,
  }) async {
    List<String> dataTypesList = [];
    for (var dataType in dataTypes) {
      dataTypesList.add(dataType.toNativeValue());
    }

    List<Map<String, dynamic>> recordList = [];
    for (var record in dataRecords) {
      recordList.add(record.toMap());
    }

    Map<String, dynamic> args = <String, dynamic>{};
    args.putIfAbsent("dataTypes", () => dataTypesList);
    args.putIfAbsent("recordList", () => recordList);
    await channel?.invokeMethod('removeDataFor', args);
  }

  @override
  Future<void> removeDataModifiedSince({
    required Set<WebsiteDataType> dataTypes,
    required DateTime date,
  }) async {
    List<String> dataTypesList = [];
    for (var dataType in dataTypes) {
      dataTypesList.add(dataType.toNativeValue());
    }

    var timestamp = date.millisecondsSinceEpoch;

    Map<String, dynamic> args = <String, dynamic>{};
    args.putIfAbsent("dataTypes", () => dataTypesList);
    args.putIfAbsent("timestamp", () => timestamp);
    await channel?.invokeMethod('removeDataModifiedSince', args);
  }

  @override
  void dispose() {
    // empty
  }
}

extension InternalWebStorageManager on WindowsWebStorageManager {
  get handleMethod => _handleMethod;
}
