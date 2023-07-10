#pragma once
#include <windows.h>
#include <string>
#include <map>
#include <minwindef.h>
#include <sysinfoapi.h>

struct BeginInfo {
	ULONGLONG beginTime;
	ULONGLONG* data;
	UINT groupCount;
	UINT dataCount;
};
class Performance {
private:
	UINT m_DefaultGroupCount;
	std::map<std::string, BeginInfo*> m_StartMap;

public:
	Performance(int defaultGroupCount) {
		m_DefaultGroupCount = defaultGroupCount;
	}
	void begin(std::string name);
	void begin(std::string name, ULONGLONG beginTickCount);
	void begin(std::string name, UINT count, ULONGLONG beginTickCount);
	void end(std::string name);
	void end(std::string name, ULONGLONG endTickCount);
};