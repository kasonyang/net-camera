#include "performance.h"

void Performance::begin(std::string name) {
	begin(name, m_DefaultGroupCount, GetTickCount64());
}

void Performance::begin(std::string name, ULONGLONG beginTickCount) {
	begin(name, m_DefaultGroupCount, beginTickCount);
}

void Performance::begin(std::string name, UINT groupCount, ULONGLONG beginTickCount) {
	auto iter = m_StartMap.find(name);
	if (iter == m_StartMap.end()) {
		BeginInfo* bi = new BeginInfo();
		bi->beginTime = beginTickCount;
		bi->groupCount = groupCount;
		bi->dataCount = 0;
		bi->data = new ULONGLONG[groupCount];
		m_StartMap[name] = bi;
		return;
	}
	else {
		iter->second->beginTime = beginTickCount;
	}
}

void Performance::end(std::string name) {
	end(name, GetTickCount64());
}
void Performance::end(std::string name, ULONGLONG endTs) {
	auto iter = m_StartMap.find(name);
	if (iter == m_StartMap.end()) {
		printf("Performance:%s not started\n", name.c_str());
		return;
	}
	BeginInfo* bi = iter->second;
	if (bi->beginTime <= 0) {
		printf("Performance: %s not started\n", name.c_str());
		return;
	}
	ULONGLONG cost = endTs - bi->beginTime;
	if (bi->dataCount >= bi->groupCount - 1) {
		ULONGLONG total = 0;
		for (int i = 0; i < bi->dataCount; i++) {
			total += bi->data[i];
		}
		total += cost;
		ULONGLONG result = total / (bi->dataCount + 1);
		printf("Performance: %s %d ms\n", name.c_str(), result);
		delete[] bi->data;
		m_StartMap.erase(name);
		return;
	}
	bi->data[bi->dataCount] = cost;
	bi->dataCount++;
	bi->beginTime = 0;
}
